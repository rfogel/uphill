package com.uphill.common.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

import java.text.SimpleDateFormat;

@UtilityClass
public class CommonUtil {

    @Getter
    private static final JsonMapper mapper;

    static {
        mapper = JsonMapper
                .builderWithJackson2Defaults()
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                .changeDefaultPropertyInclusion(incl -> incl.withContentInclusion(JsonInclude.Include.NON_NULL))
                .enable(tools.jackson.databind.SerializationFeature.INDENT_OUTPUT)
                .disable(tools.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DateTimeFeature.WRITE_DATES_WITH_ZONE_ID)
                .defaultDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"))
                .build();
    }
}
