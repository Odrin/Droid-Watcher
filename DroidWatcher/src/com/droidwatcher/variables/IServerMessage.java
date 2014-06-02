package com.droidwatcher.variables;

import com.droidwatcher.lib.MessageType;

public interface IServerMessage {
	public MessageType getType();
	public String getJSONString();
}
