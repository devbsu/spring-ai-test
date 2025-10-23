package com.example.demo.dto;

import java.util.List;

import lombok.Data;

@Data
public class Hotel {
    private String city;
    private List<String> names;
}
