package com.zettix.dto.response;

import com.zettix.entity.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String token;
    private String type;
    private User user;
}
