package ai.serenade.treesitter;

public class Connection {
	String url;
	Context keyValuePairs;
	
	public Connection(String url, Context ctx) {
		this.url = url;
		this.keyValuePairs = ctx;
	}

	public String toString() {
		return String.format("Connection(URL: '%s' KVS: %s)\n", url, keyValuePairs.toString());
	}

}
