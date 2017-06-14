/*
 * Copyright 2017 Josue Gontijo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.joshworks.stream.client.sse;

import java.util.regex.Pattern;

/**
 * <a href="https://html.spec.whatwg.org/multipage/comms.html#server-sent-events">Interprets an event stream</a>
 */
public class EventStreamParser {
    private static final String DATA = "data";
    private static final String ID = "id";
    private static final String EVENT = "event";
    private static final String RETRY = "autoReconnect";

    private static final String DEFAULT_EVENT = "message";
    private static final String LINE_BREAK = "\n";
    private static final String EMPTY_STRING = "";
    private static final String WHITESPACE = " ";
    private static final String COLON = ":";
    private static final Pattern NUMERIC = Pattern.compile("^[\\d]+$");


    private StringBuffer data = new StringBuffer();
    private String lastEventId;
    private String eventName = DEFAULT_EVENT;
    private String origin = "";

    private final SSEConnection connection;

    EventStreamParser(SSEConnection connection) {
        this.connection = connection;
    }

    public void lines(String lines) {
        String[] lineArray = lines.split(LINE_BREAK, -1);
        for (String line : lineArray) {
            parse(line);
        }
    }

    public void parse(String line) {
        int colonIndex;
        if (line.trim().isEmpty()) {
            dispatchEvent();
        } else if (line.startsWith(COLON)) {
            // DO NOTHING
        } else if ((colonIndex = line.indexOf(COLON)) != -1) {
            String field = line.substring(0, colonIndex);
            String value = line.substring(colonIndex + 1).replaceFirst(WHITESPACE, EMPTY_STRING);
            processField(field, value);
        } else {
            processField(line.trim(), EMPTY_STRING);
        }
    }

    private void processField(String field, String value) {
        if (DATA.equals(field)) {
            data.append(value);
        } else if (ID.equals(field)) {
            lastEventId = value;
        } else if (EVENT.equals(field)) {
            eventName = value;
        } else if (RETRY.equals(field) && isNumber(value)) {
            connection.retryAfter(Long.parseLong(value));
        }
    }

    private boolean isNumber(String value) {
        return NUMERIC.matcher(value).matches();
    }

    private void dispatchEvent() {
        if (data.length() == 0) {
            return;
        }
        EventData message = new EventData(
                removeLineBreak(data.toString()),
                removeLineBreak(lastEventId),
                removeLineBreak(eventName),
                removeLineBreak(origin));

        connection.lastEventId = removeLineBreak(lastEventId);
        try {
            connection.callback.onEvent(message);
        } catch (Exception e) {
            connection.callback.onError(e);
        }
        data = new StringBuffer();
        eventName = DEFAULT_EVENT;
    }

    private String removeLineBreak(String value) {
        if(value == null) {
            return null;
        }
        if (value.endsWith(LINE_BREAK)) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

}
