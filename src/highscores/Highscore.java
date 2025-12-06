package highscores;

public class Highscore implements Comparable<Highscore> {
	private final String name;
	private final long timeMillis;
	private final int moves;

	public Highscore(String name, long timeMillis, int moves) {
		this.name = name;
		this.timeMillis = timeMillis;
		this.moves = moves;
	}

	public String getName() {
		return name;
	}

	public long getTimeMillis() {
		return timeMillis;
	}

	public int getMoves() {
		return moves;
	}

	@Override
	public int compareTo(Highscore other) {
		int cmp = Long.compare(this.timeMillis, other.timeMillis);
		if (cmp != 0)
			return cmp;
		return Integer.compare(this.moves, other.moves);
	}

	@Override
	public String toString() {
		return name + ";" + timeMillis + ";" + moves;
	}

	public static Highscore fromString(String line) throws IllegalArgumentException {
		String[] parts = line.split(";");
		if (parts.length != 3)
			throw new IllegalArgumentException("Linha inválida de highscore: " + line);
		String name = parts[0];
		long time = Long.parseLong(parts[1]);
		int moves = Integer.parseInt(parts[2]);
		return new Highscore(name, time, moves);
	}
}
