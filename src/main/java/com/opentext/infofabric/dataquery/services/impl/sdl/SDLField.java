/**
 * Copyright 2018 Liaison Technologies, Inc.
 * This software is the confidential and proprietary information of
 * Liaison Technologies, Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Liaison Technologies.
 */
package com.opentext.infofabric.dataquery.services.impl.sdl;

import com.opentext.infofabric.datamodel.models.attributes.Attribute;
import com.opentext.infofabric.dataquery.DataqueryConstants;

import java.util.HashMap;
import java.util.Map;

public class SDLField {
    protected static final Map<String, String> MODEL_TO_SDL_TYPE = new HashMap<>();
    public static final String SCALAR_STRING = "String";
    public static final String SCALAR_FLOAT = "Float";
    public static final String SCALAR_BOOLEAN = "Boolean";
    public static final String SCALAR_INT = "Int";
    public static final String SCALAR_SHORT = "Short";
    public static final String SCALAR_LONG = "Long";
    public static final String SCALAR_BYTE = "Byte";
    public static final String MANDATORY_FIELD = "!";

    static {
        //GraphQL build-in types
        MODEL_TO_SDL_TYPE.put("string", SCALAR_STRING);
        MODEL_TO_SDL_TYPE.put("cistring", SCALAR_STRING);
        MODEL_TO_SDL_TYPE.put("boolean", SCALAR_BOOLEAN);
        MODEL_TO_SDL_TYPE.put("integer", SCALAR_INT);
        MODEL_TO_SDL_TYPE.put("short", SCALAR_SHORT);
        MODEL_TO_SDL_TYPE.put("long", SCALAR_LONG);
        MODEL_TO_SDL_TYPE.put("float", SCALAR_FLOAT);
        MODEL_TO_SDL_TYPE.put("double", SCALAR_FLOAT);
        MODEL_TO_SDL_TYPE.put("byte", SCALAR_BYTE);
        MODEL_TO_SDL_TYPE.put("other", SCALAR_STRING);

        //Custom scalar types
        MODEL_TO_SDL_TYPE.put("base64binary", DataqueryConstants.SCALAR_BASE64BINARY);
        MODEL_TO_SDL_TYPE.put("date", DataqueryConstants.SCALAR_DATE);
        MODEL_TO_SDL_TYPE.put("createdtime", DataqueryConstants.SCALAR_TIMESTAMP);
        MODEL_TO_SDL_TYPE.put("processedtime", DataqueryConstants.SCALAR_TIMESTAMP);
        MODEL_TO_SDL_TYPE.put("modifiedtime", DataqueryConstants.SCALAR_TIMESTAMP);
        MODEL_TO_SDL_TYPE.put("datetime", DataqueryConstants.SCALAR_TIMESTAMP);
        MODEL_TO_SDL_TYPE.put("time", DataqueryConstants.SCALAR_TIME);
    }

    private Attribute attribute;

    public SDLField( Attribute attribute) {
        this.attribute = attribute;
    }

    public static SDLField instance(Attribute attribute) {
        return new SDLField(attribute);
    }

    public String toString() {
        return String.format("%s%s: %s%s", DataqueryConstants.TAB_BREAK, attribute.getName(),
                MODEL_TO_SDL_TYPE.get(attribute.getType().name().toLowerCase()),
                attribute.getNullAllowed() ? "" : "!");
    }

    public String getEqualityKeys() {
        return String.format("%s%s: %s%s", DataqueryConstants.TAB_BREAK, attribute.getName(),
                MODEL_TO_SDL_TYPE.get(attribute.getType().name().toLowerCase()),
                MANDATORY_FIELD);
    }

    public String getAttribute() {
        return String.format("%s%s: %s", DataqueryConstants.TAB_BREAK, attribute.getName(),
                MODEL_TO_SDL_TYPE.get(attribute.getType().name().toLowerCase()));
    }
}
