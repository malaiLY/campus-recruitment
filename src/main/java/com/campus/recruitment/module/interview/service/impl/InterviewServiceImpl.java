package com.campus.recruitment.module.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.recruitment.common.constant.RabbitMQConstants;
import com.campus.recruitment.common.constant.RedisConstants;
import com.campus.recruitment.common.context.LoginUserContext;
import com.campus.recruitment.common.context.LoginUserContext.LoginUser;
import com.campus.recruitment.common.enums.ApplicationStatus;
import com.campus.recruitment.common.enums.InterviewBookingStatus;
import com.campus.recruitment.common.enums.InterviewSlotStatus;
import com.campus.recruitment.common.enums.UserType;
import com.campus.recruitment.common.exception.BizException;
import com.campus.recruitment.common.exception.ErrorCode;
import com.campus.recruitment.entity.CompanyProfile;
import com.campus.recruitment.entity.InterviewBooking;
import com.campus.recruitment.entity.InterviewSlot;
import com.campus.recruitment.entity.Job;
import com.campus.recruitment.entity.JobApplication;
import com.campus.recruitment.entity.StudentProfile;
import com.campus.recruitment.mapper.CompanyProfileMapper;
import com.campus.recruitment.mapper.InterviewBookingMapper;
import com.campus.recruitment.mapper.InterviewSlotMapper;
import com.campus.recruitment.mapper.JobApplicationMapper;
import com.campus.recruitment.mapper.JobMapper;
import com.campus.recruitment.mapper.StudentProfileMapper;
import com.campus.recruitment.module.interview.dto.BookInterviewRequest;
import com.campus.recruitment.module.interview.dto.CreateInterviewSlotRequest;
import com.campus.recruitment.module.interview.service.InterviewService;
import com.campus.recruitment.module.interview.vo.InterviewBookingVO;
import com.campus.recruitment.module.interview.vo.InterviewSlotVO;
import com.campus.recruitment.module.interview.vo.MyBookingVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.campus.recruitment.common.mq.OutboxService;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private final InterviewSlotMapper interviewSlotMapper;
    private final InterviewBookingMapper interviewBookingMapper;
    private final JobApplicationMapper jobApplicationMapper;
    private final JobMapper jobMapper;
    private final CompanyProfileMapper companyProfileMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> interviewBookingScript;
    private final OutboxService outboxService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSlot(CreateInterviewSlotRequest request) {
        Long userId = LoginUserContext.getUserId();
        verifyCompanyUser(userId);

        CompanyProfile company = companyProfileMapper.selectOne(
                new LambdaQueryWrapper<CompanyProfile>().eq(CompanyProfile::getUserId, userId));
        if (company == null) {
            throw new BizException(ErrorCode.COMPANY_UNVERIFIED);
        }

        Job job = jobMapper.selectById(request.getJobId());
        if (job == null) {
            throw new BizException(ErrorCode.JOB_NOT_EXIST);
        }
        if (!company.getId().equals(job.getCompanyId())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }

        InterviewSlot slot = new InterviewSlot();
        BeanUtils.copyProperties(request, slot);
        slot.setCompanyId(company.getId());
        slot.setRemainCount(request.getCapacity());
        slot.setStatus(InterviewSlotStatus.OPEN.name());
        slot.setCreateTime(LocalDateTime.now());
        slot.setUpdateTime(LocalDateTime.now());
        slot.setCreateBy(userId);
        slot.setUpdateBy(userId);

        interviewSlotMapper.insert(slot);

        String stockKey = RedisConstants.INTERVIEW_SLOT_STOCK_PREFIX + slot.getId();
        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(slot.getCapacity()));

        return slot.getId();
    }

    @Override
    public List<InterviewSlotVO> getCompanySlots(Long jobId) {
        Long userId = LoginUserContext.getUserId();
        CompanyProfile company = companyProfileMapper.selectOne(
                new LambdaQueryWrapper<CompanyProfile>().eq(CompanyProfile::getUserId, userId));
        if (company == null) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<InterviewSlot> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InterviewSlot::getCompanyId, company.getId());
        if (jobId != null) {
            queryWrapper.eq(InterviewSlot::getJobId, jobId);
        }
        queryWrapper.orderByDesc(InterviewSlot::getCreateTime);

        List<InterviewSlot> slots = interviewSlotMapper.selectList(queryWrapper);

        Map<Long, String> jobTitleMap = getJobTitleMap(slots);

        return slots.stream().map(slot -> {
            InterviewSlotVO vo = convertToSlotVO(slot);
            vo.setJobTitle(jobTitleMap.get(slot.getJobId()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeSlot(Long slotId) {
        Long userId = LoginUserContext.getUserId();
        InterviewSlot slot = getSlotByIdAndCheckOwner(slotId, userId);

        slot.setStatus(InterviewSlotStatus.CLOSED.name());
        slot.setUpdateTime(LocalDateTime.now());
        slot.setUpdateBy(userId);
        interviewSlotMapper.updateById(slot);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void openSlot(Long slotId) {
        Long userId = LoginUserContext.getUserId();
        InterviewSlot slot = getSlotByIdAndCheckOwner(slotId, userId);

        slot.setStatus(InterviewSlotStatus.OPEN.name());
        slot.setUpdateTime(LocalDateTime.now());
        slot.setUpdateBy(userId);
        interviewSlotMapper.updateById(slot);
    }

    @Override
    public List<InterviewBookingVO> getBookingsForSlot(Long slotId) {
        Long userId = LoginUserContext.getUserId();
        InterviewSlot slot = getSlotByIdAndCheckOwner(slotId, userId);

        LambdaQueryWrapper<InterviewBooking> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InterviewBooking::getSlotId, slotId);
        queryWrapper.orderByDesc(InterviewBooking::getCreateTime);

        List<InterviewBooking> bookings = interviewBookingMapper.selectList(queryWrapper);
        if (bookings.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> studentIds = bookings.stream().map(InterviewBooking::getStudentId).distinct().collect(Collectors.toList());
        List<Long> applicationIds = bookings.stream().map(InterviewBooking::getApplicationId).distinct().collect(Collectors.toList());
        List<Long> jobIds = bookings.stream().map(InterviewBooking::getJobId).distinct().collect(Collectors.toList());

        Map<Long, StudentProfile> studentMap = studentProfileMapper.selectList(
                new LambdaQueryWrapper<StudentProfile>().in(StudentProfile::getUserId, studentIds))
                .stream().collect(Collectors.toMap(StudentProfile::getUserId, s -> s));
        Map<Long, Job> jobMap = jobMapper.selectBatchIds(jobIds).stream()
                .collect(Collectors.toMap(Job::getId, j -> j));

        return bookings.stream().map(booking -> {
            InterviewBookingVO vo = new InterviewBookingVO();
            vo.setId(booking.getId());
            vo.setSlotId(booking.getSlotId());
            vo.setApplicationId(booking.getApplicationId());
            vo.setStudentId(booking.getStudentId());
            vo.setStatus(booking.getStatus());
            vo.setBookingTime(booking.getBookingTime());

            StudentProfile student = studentMap.get(booking.getStudentId());
            if (student != null) {
                vo.setStudentName(student.getRealName());
                vo.setSchool(student.getSchool());
            }

            Job job = jobMap.get(booking.getJobId());
            if (job != null) {
                vo.setJobTitle(job.getTitle());
            }

            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<InterviewSlotVO> getAvailableSlots(Long jobId) {
        LambdaQueryWrapper<InterviewSlot> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InterviewSlot::getJobId, jobId);
        queryWrapper.eq(InterviewSlot::getStatus, InterviewSlotStatus.OPEN.name());
        queryWrapper.gt(InterviewSlot::getRemainCount, 0);
        queryWrapper.orderByDesc(InterviewSlot::getCreateTime);

        List<InterviewSlot> slots = interviewSlotMapper.selectList(queryWrapper);
        if (slots.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> jobIds = slots.stream().map(InterviewSlot::getJobId).distinct().collect(Collectors.toList());
        Map<Long, String> jobTitleMap = jobMapper.selectBatchIds(jobIds).stream()
                .collect(Collectors.toMap(Job::getId, Job::getTitle));

        return slots.stream().map(slot -> {
            InterviewSlotVO vo = convertToSlotVO(slot);
            vo.setJobTitle(jobTitleMap.get(slot.getJobId()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bookInterview(BookInterviewRequest request) {
        Long userId = LoginUserContext.getUserId();
        verifyStudentUser();

        JobApplication application = jobApplicationMapper.selectById(request.getApplicationId());
        if (application == null) {
            throw new BizException(ErrorCode.APPLICATION_NOT_EXIST);
        }
        if (!application.getStudentId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        if (!ApplicationStatus.INTERVIEW_INVITED.name().equals(application.getStatus())) {
            throw new BizException(ErrorCode.INTERVIEW_NO_INVITE);
        }

        InterviewSlot slot = interviewSlotMapper.selectById(request.getSlotId());
        if (slot == null) {
            throw new BizException(ErrorCode.INTERVIEW_SLOT_NOT_EXIST);
        }
        if (!InterviewSlotStatus.OPEN.name().equals(slot.getStatus())) {
            throw new BizException(ErrorCode.INTERVIEW_EXPIRED);
        }

        if (!slot.getJobId().equals(application.getJobId())
                || !slot.getCompanyId().equals(application.getCompanyId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "面试场次与投递记录不匹配");
        }

        String stockKey = RedisConstants.INTERVIEW_SLOT_STOCK_PREFIX + request.getSlotId();
        String userBookingKey = RedisConstants.INTERVIEW_BOOKING_USER_PREFIX + request.getSlotId() + ":" + userId;

        Long result = stringRedisTemplate.execute(
                interviewBookingScript,
                List.of(stockKey, userBookingKey),
                String.valueOf(userId),
                String.valueOf(RedisConstants.INTERVIEW_BOOKING_TTL_SECONDS));

        if (result == -2) {
            throw new BizException(ErrorCode.INTERVIEW_DUPLICATE);
        }
        if (result == -1) {
            throw new BizException(ErrorCode.INTERVIEW_FULL);
        }

        try {
            InterviewBooking booking = new InterviewBooking();
            booking.setSlotId(request.getSlotId());
            booking.setApplicationId(request.getApplicationId());
            booking.setStudentId(userId);
            booking.setCompanyId(slot.getCompanyId());
            booking.setJobId(slot.getJobId());
            booking.setStatus(InterviewBookingStatus.BOOKED.name());
            booking.setBookingTime(LocalDateTime.now());
            booking.setCreateTime(LocalDateTime.now());
            booking.setUpdateTime(LocalDateTime.now());
            booking.setCreateBy(userId);
            booking.setUpdateBy(userId);
            interviewBookingMapper.insert(booking);

            int updated = interviewSlotMapper.decrementRemainCount(request.getSlotId());
            if (updated == 0) {
                throw new BizException(ErrorCode.INTERVIEW_FULL);
            }

            application.setStatus(ApplicationStatus.BOOKED.name());
            application.setUpdateTime(LocalDateTime.now());
            jobApplicationMapper.updateById(application);

            sendBookingNotification(userId, slot, application);
        } catch (Exception e) {
            stringRedisTemplate.opsForValue().increment(stockKey);
            stringRedisTemplate.delete(userBookingKey);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelBooking(Long bookingId) {
        Long userId = LoginUserContext.getUserId();
        verifyStudentUser();

        InterviewBooking booking = interviewBookingMapper.selectById(bookingId);
        if (booking == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "预约记录不存在");
        }
        if (!booking.getStudentId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }

        int updated = interviewBookingMapper.cancelIfBooked(bookingId, userId);
        if (updated == 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "该预约已取消或已完成");
        }

        String stockKey = RedisConstants.INTERVIEW_SLOT_STOCK_PREFIX + booking.getSlotId();
        String userBookingKey = RedisConstants.INTERVIEW_BOOKING_USER_PREFIX + booking.getSlotId() + ":" + userId;

        boolean stockReleased = false;
        try {
            stringRedisTemplate.opsForValue().increment(stockKey);
            stockReleased = true;
            stringRedisTemplate.delete(userBookingKey);

            interviewSlotMapper.incrementRemainCount(booking.getSlotId());

            JobApplication application = jobApplicationMapper.selectById(booking.getApplicationId());
            if (application != null && ApplicationStatus.BOOKED.name().equals(application.getStatus())) {
                application.setStatus(ApplicationStatus.INTERVIEW_INVITED.name());
                application.setUpdateTime(LocalDateTime.now());
                jobApplicationMapper.updateById(application);
            }
        } catch (Exception e) {
            if (stockReleased) {
                try {
                    stringRedisTemplate.opsForValue().decrement(stockKey);
                    stringRedisTemplate.opsForValue().set(
                            userBookingKey,
                            userId.toString(),
                            RedisConstants.INTERVIEW_BOOKING_TTL_SECONDS,
                            TimeUnit.SECONDS);
                } catch (Exception redisException) {
                    log.warn("Restore Redis booking state failed: bookingId={}, error={}", bookingId, redisException.getMessage());
                }
            }
            throw e;
        }
    }

    @Override
    public List<MyBookingVO> getMyBookings() {
        Long userId = LoginUserContext.getUserId();

        LambdaQueryWrapper<InterviewBooking> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InterviewBooking::getStudentId, userId);
        queryWrapper.orderByDesc(InterviewBooking::getCreateTime);

        List<InterviewBooking> bookings = interviewBookingMapper.selectList(queryWrapper);
        if (bookings.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> slotIds = bookings.stream().map(InterviewBooking::getSlotId).distinct().collect(Collectors.toList());
        List<Long> jobIds = bookings.stream().map(InterviewBooking::getJobId).distinct().collect(Collectors.toList());

        Map<Long, InterviewSlot> slotMap = interviewSlotMapper.selectBatchIds(slotIds).stream()
                .collect(Collectors.toMap(InterviewSlot::getId, s -> s));
        Map<Long, Job> jobMap = jobMapper.selectBatchIds(jobIds).stream()
                .collect(Collectors.toMap(Job::getId, j -> j));

        List<Long> companyIds = bookings.stream().map(InterviewBooking::getCompanyId).distinct().collect(Collectors.toList());
        Map<Long, String> companyMap = companyProfileMapper.selectBatchIds(companyIds).stream()
                .collect(Collectors.toMap(CompanyProfile::getId, CompanyProfile::getCompanyName));

        return bookings.stream().map(booking -> {
            MyBookingVO vo = new MyBookingVO();
            vo.setId(booking.getId());
            vo.setSlotId(booking.getSlotId());
            vo.setJobId(booking.getJobId());
            vo.setStatus(booking.getStatus());
            vo.setBookingTime(booking.getBookingTime());

            InterviewSlot slot = slotMap.get(booking.getSlotId());
            if (slot != null) {
                vo.setSlotTitle(slot.getTitle());
                vo.setStartTime(slot.getStartTime());
                vo.setEndTime(slot.getEndTime());
                vo.setInterviewType(slot.getInterviewType());
                vo.setLocation(slot.getLocation());
            }

            Job job = jobMap.get(booking.getJobId());
            if (job != null) {
                vo.setJobTitle(job.getTitle());
            }

            vo.setCompanyName(companyMap.get(booking.getCompanyId()));

            return vo;
        }).collect(Collectors.toList());
    }

    private void verifyCompanyUser(Long userId) {
        String userType = LoginUserContext.get().getUserType();
        if (!UserType.COMPANY.name().equals(userType)) {
            throw new BizException(ErrorCode.USER_TYPE_MISMATCH);
        }
    }

    private void verifyStudentUser() {
        String userType = LoginUserContext.get().getUserType();
        if (!UserType.STUDENT.name().equals(userType)) {
            throw new BizException(ErrorCode.USER_TYPE_MISMATCH);
        }
    }

    private InterviewSlot getSlotByIdAndCheckOwner(Long slotId, Long userId) {
        InterviewSlot slot = interviewSlotMapper.selectById(slotId);
        if (slot == null) {
            throw new BizException(ErrorCode.INTERVIEW_SLOT_NOT_EXIST);
        }

        CompanyProfile company = companyProfileMapper.selectOne(
                new LambdaQueryWrapper<CompanyProfile>().eq(CompanyProfile::getUserId, userId));
        if (company == null || !company.getId().equals(slot.getCompanyId())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }

        return slot;
    }

    private Map<Long, String> getJobTitleMap(List<InterviewSlot> slots) {
        List<Long> jobIds = slots.stream().map(InterviewSlot::getJobId).distinct().collect(Collectors.toList());
        return jobMapper.selectBatchIds(jobIds).stream()
                .collect(Collectors.toMap(Job::getId, Job::getTitle));
    }

    private InterviewSlotVO convertToSlotVO(InterviewSlot slot) {
        InterviewSlotVO vo = new InterviewSlotVO();
        BeanUtils.copyProperties(slot, vo);
        return vo;
    }

    private void sendBookingNotification(Long studentId, InterviewSlot slot, JobApplication application) {
        String messageId = java.util.UUID.randomUUID().toString();
        Map<String, Object> mqMessage = Map.of(
                "messageId", messageId,
                "receiverId", studentId,
                "senderId", application.getCompanyId(),
                "messageType", "INTERVIEW",
                "title", "面试预约成功",
                "content", "您已成功预约面试时间段: " + slot.getStartTime() + " 至 " + slot.getEndTime(),
                "businessType", "INTERVIEW",
                "businessId", application.getId()
        );
        outboxService.sendAfterCommit(
                RabbitMQConstants.NOTIFY_EXCHANGE,
                RabbitMQConstants.NOTIFY_ROUTING_KEY,
                mqMessage, messageId, "INTERVIEW", application.getId());
        log.info("发送面试预约通知MQ消息(Outbox): messageId={}, studentId={}", messageId, studentId);
    }
}
