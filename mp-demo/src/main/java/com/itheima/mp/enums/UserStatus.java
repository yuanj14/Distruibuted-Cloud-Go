package com.itheima.mp.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UserStatus {
    NORMAL(1, "正常"),
    FROZEN(2, "冻结"),
    ;
    @EnumValue
    private final int value;
    @JsonValue
    private final String status;

    UserStatus(int value, String status) {
        this.value = value;
        this.status = status;
    }
}
