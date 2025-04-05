package org.example.demo1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestObject {
    // 基本数据类型
    private int id = 1001;
    private double price = 99.99;
    private boolean active = true;
    
    // 字符串
    private String name = "测试对象";
    private String description = "这是一个用于测试JSON复制功能的复杂对象";
    
    // 数组
    private int[] numbers = {1, 2, 3, 4, 5};
    private String[] tags = {"测试", "示例", "JSON"};
    
    // 集合
    private List<String> comments = new ArrayList<>();
    private Map<String, Integer> scores = new HashMap<>();
    
    // 嵌套对象
    private Address address = new Address();
    private User creator = new User();
    
    public static class Address {
        private String city = "北京";
        private String street = "朝阳区";
        private String zipCode = "100000";
        
        // getter和setter省略
    }
    
    public static class User {
        private int userId = 123;
        private String username = "admin";
        private String email = "admin@example.com";
        private String[] roles = {"管理员", "开发者"};
        
        // getter和setter省略
    }
    
    public TestObject() {
        // 初始化集合
        comments.add("评论1");
        comments.add("评论2");
        comments.add("评论3");
        
        scores.put("语文", 90);
        scores.put("数学", 95);
        scores.put("英语", 85);
    }
    
    public static void main(String[] args) {
        TestObject testObject = new TestObject();
        System.out.println("创建测试对象完成，请在调试模式中查看此对象并测试JSON复制功能");
        // 设置断点在这里，然后在变量窗口中右键点击testObject测试JSON复制功能
    }
} 