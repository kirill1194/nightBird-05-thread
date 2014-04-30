package Server;

import java.io.Serializable;

import Enums.Action;

public class Response implements Serializable{

	private static final long serialVersionUID = 1L;

	private Action action;
	
	public Response(Action action_) {
		action = action_;
	}
	
	public Action getAction() {
		return action;
	}
}
