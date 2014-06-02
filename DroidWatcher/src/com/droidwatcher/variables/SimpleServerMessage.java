package com.droidwatcher.variables;

import com.droidwatcher.lib.MessageType;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SimpleServerMessage implements IServerMessage {
    public MessageType type;
    private Map<String, Object> map;

    public SimpleServerMessage(MessageType type) {
        this.type = type;
        this.map = new HashMap<String, Object>();
    }

    public SimpleServerMessage addParam(String key, Object value) {
        map.put(key, value);
        return this;
    }

    public String getJSONString() {
        try {
            JSONObject obj = new JSONObject(map);
            obj.put("type", type.name());

            return obj.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }
    }

	@Override
	public MessageType getType() {
		return type;
	}
}
