package ai.serenade.treesitter;

public class SiardError extends Exception {

	private static final long serialVersionUID = 1L;
	String reason;

	public SiardError(String reason) {
		this.reason = reason;
	}
	
	public String getReason() {
		return reason;
	}

}
