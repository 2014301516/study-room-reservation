package com.studyroom.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyroom.entity.User;
import com.studyroom.mapper.UserMapper;
import com.studyroom.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public Map<String, Object> register(Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String realName = body.get("real_name");
        String studentId = body.get("student_id");

        if (username == null || password == null || realName == null || studentId == null) {
            return Map.of("code", 400, "message", "请填写必填字段");
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username)) > 0
                || userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getStudentId, studentId)) > 0) {
            return Map.of("code", 400, "message", "用户名或学号已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(encoder.encode(password));
        user.setRealName(realName);
        user.setStudentId(studentId);
        user.setPhone(body.getOrDefault("phone", ""));
        user.setEmail(body.getOrDefault("email", ""));
        userMapper.insert(user);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("message", "注册成功");
        return result;
    }

    public Map<String, Object> login(Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null || !encoder.matches(password, user.getPassword())) {
            return Map.of("code", 400, "message", "用户名或密码错误");
        }
        if ("banned".equals(user.getStatus())) {
            return Map.of("code", 403, "message", "账号已被封禁");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole(), user.getRealName());

        Map<String, Object> userData = new LinkedHashMap<>();
        userData.put("id", user.getId());
        userData.put("username", user.getUsername());
        userData.put("role", user.getRole());
        userData.put("real_name", user.getRealName());
        userData.put("student_id", user.getStudentId());
        userData.put("avatar", user.getAvatar());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("user", userData);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", data);
        return result;
    }

    public Map<String, Object> getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return Map.of("code", 404, "message", "用户不存在");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("real_name", user.getRealName());
        data.put("student_id", user.getStudentId());
        data.put("phone", user.getPhone());
        data.put("email", user.getEmail());
        data.put("role", user.getRole());
        data.put("avatar", user.getAvatar());
        data.put("status", user.getStatus());
        data.put("created_at", user.getCreatedAt());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", data);
        return result;
    }

    public Map<String, Object> updateProfile(Long userId, Map<String, String> body) {
        User user = userMapper.selectById(userId);
        if (user == null) return Map.of("code", 404, "message", "用户不存在");
        if (body.containsKey("phone")) user.setPhone(body.get("phone"));
        if (body.containsKey("email")) user.setEmail(body.get("email"));
        userMapper.updateById(user);
        return Map.of("code", 200, "message", "更新成功");
    }
}
