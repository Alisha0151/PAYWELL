package com.paywell.common;

import java.util.LinkedHashMap;
import java.util.Map;

/** Business/validation failure that maps to a problem+json response. Never carries secrets. */
public class ApiException extends RuntimeException {
    private final ErrorCode code;
    private final transient Map<String, Object> properties = new LinkedHashMap<>();

    public ApiException(ErrorCode code) {
        this(code, code.title());
    }

    public ApiException(ErrorCode code, String detail) {
        super(detail);
        this.code = code;
    }

    public ApiException with(String key, Object value) {
        properties.put(key, value);
        return this;
    }

    public ErrorCode code() { return code; }
    public Map<String, Object> properties() { return properties; }
}
