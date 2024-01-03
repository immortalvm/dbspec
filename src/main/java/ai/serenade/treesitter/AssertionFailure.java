package ai.serenade.treesitter;

public class AssertionFailure extends Error {
	private static final long serialVersionUID = 1L;
	int startPos;
	int endPos;
	
	public AssertionFailure(int startPos, int endPos) {
		this.startPos = startPos;
		this.endPos = endPos;
	}

}
