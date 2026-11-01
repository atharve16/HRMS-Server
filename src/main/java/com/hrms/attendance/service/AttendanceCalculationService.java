package com.hrms.attendance.service;

import com.hrms.attendance.model.*;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class AttendanceCalculationService {
    public void recalculate(AttendanceRecord attendance) {
        List<AttendanceSession> sessions = attendance.getSessions() == null ? List.of() : attendance.getSessions();
        long worked = 0, breaks = 0;
        Instant previousOut = null, firstIn = null, lastOut = null;
        for (AttendanceSession session : sessions) {
            if (session.getCheckInAt() == null) throw invalid();
            if (firstIn == null || session.getCheckInAt().isBefore(firstIn)) firstIn = session.getCheckInAt();
            if (session.getStatus() == SessionStatus.COMPLETED) {
                if (session.getCheckOutAt() == null || !session.getCheckOutAt().isAfter(session.getCheckInAt())) throw invalid();
                if (previousOut != null && session.getCheckInAt().isBefore(previousOut)) throw invalid();
                worked += Duration.between(session.getCheckInAt(), session.getCheckOutAt()).toMinutes();
                if (previousOut != null) breaks += Duration.between(previousOut, session.getCheckInAt()).toMinutes();
                previousOut = session.getCheckOutAt();
                if (lastOut == null || session.getCheckOutAt().isAfter(lastOut)) lastOut = session.getCheckOutAt();
            }
        }
        ShiftSnapshot shift = attendance.getShiftSnapshot();
        long late = firstIn == null ? 0 : Math.max(0, Duration.between(shift.getScheduledStartAt().plus(Duration.ofMinutes(shift.getGracePeriodMinutes())), firstIn).toMinutes());
        long early = lastOut == null ? 0 : Math.max(0, Duration.between(lastOut, shift.getScheduledEndAt().minus(Duration.ofMinutes(shift.getEarlyDepartureGraceMinutes()))).toMinutes());
        long overtime = shift.isFlexible() || lastOut == null ? 0 : Math.max(0, Duration.between(shift.getScheduledEndAt(), lastOut).toMinutes());
        attendance.setTotalWorkedMinutes(worked); attendance.setTotalBreakMinutes(Math.max(0, breaks));
        attendance.setLateByMinutes(late); attendance.setEarlyDepartureByMinutes(early); attendance.setOvertimeMinutes(overtime);
        boolean open = sessions.stream().anyMatch(s -> s.getStatus() == SessionStatus.OPEN);
        attendance.setAttendanceStatus(open ? AttendanceStatus.INCOMPLETE : (worked >= expectedMinutes(shift) ? AttendanceStatus.PRESENT : AttendanceStatus.PARTIAL));
    }
    private long expectedMinutes(ShiftSnapshot shift) { return Math.max(0, Duration.between(shift.getScheduledStartAt(), shift.getScheduledEndAt()).toMinutes() - shift.getExpectedBreakMinutes()); }
    private IllegalArgumentException invalid() { return new IllegalArgumentException("Attendance session timestamps are invalid"); }
}
