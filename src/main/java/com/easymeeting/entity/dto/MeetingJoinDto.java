package com.easymeeting.entity.dto;

import java.io.Serializable;
import java.util.List;

public class MeetingJoinDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private MeetingMemberDto newMember;
    private List<MeetingMemberDto> meetingMemberDtoList;

    public List<MeetingMemberDto> getMeetingMemberDtoList() {
        return meetingMemberDtoList;
    }

    public void setMeetingMemberDtoList(List<MeetingMemberDto> meetingMemberDtoList) {
        this.meetingMemberDtoList = meetingMemberDtoList;
    }

    public MeetingMemberDto getNewMember() {
        return newMember;
    }

    public void setNewMember(MeetingMemberDto newMember) {
        this.newMember = newMember;
    }
}
