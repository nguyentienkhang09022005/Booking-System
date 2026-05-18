package com.example.java_service.Constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PredefinedNotebookStatus {
    // Template constants
    public static final String TEMPLATE_CUSTOM = "CUSTOM";
    public static final String TEMPLATE_TRIP_PLANNER = "TRIP_PLANNER";
    public static final String TEMPLATE_PACKING_LIST = "PACKING_LIST";
    public static final String TEMPLATE_EXPENSE = "EXPENSE";

    // Block type constants
    public static final String BLOCK_TYPE_TEXT = "TEXT";
    public static final String BLOCK_TYPE_IMAGE = "IMAGE";
    public static final String BLOCK_TYPE_HEADING = "HEADING";
    public static final String BLOCK_TYPE_LIST = "LIST";
    public static final String BLOCK_TYPE_CHECKLIST = "CHECKLIST";
    public static final String BLOCK_TYPE_QUOTE = "QUOTE";
    public static final String BLOCK_TYPE_LINK = "LINK";
    public static final String BLOCK_TYPE_TABLE = "TABLE";
}