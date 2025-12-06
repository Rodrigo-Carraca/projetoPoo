package highscores;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class HighscoreManager {

	private static final String DEFAULT_DIR = "data";
	private static final String DEFAULT_FILE = "highscores.txt";
	private final Path filePath;

	private final List<Highscore> scores = new ArrayList<>();

	public HighscoreManager() {
		this(DEFAULT_DIR, DEFAULT_FILE);
	}

	public HighscoreManager(String dir, String filename) {
		Path folder = Paths.get(dir);
		if (!Files.exists(folder)) {
			try {
				Files.createDirectories(folder);
			} catch (IOException e) {
				System.err.println("Não foi possível criar a pasta para highscores: " + e.getMessage());
			}
		}
		this.filePath = folder.resolve(filename);
		load();
	}

	//Adiciona um novo highscore, ordena e mantém apenas os top 10. Grava
	public synchronized void addScore(String name, long timeMillis, int moves) {
		if (name == null || name.trim().isEmpty())
			name = "Jogador";
		scores.add(new Highscore(name.trim(), timeMillis, moves));
		Collections.sort(scores);
		// Mantém só os 10 primeiros
		if (scores.size() > 10) {
			List<Highscore> top = new ArrayList<>(scores.subList(0, 10));
			scores.clear();
			scores.addAll(top);
		}
		save();
	}

	public synchronized List<Highscore> getScores() {
		return new ArrayList<>(scores);
	}

	private void load() {
		scores.clear();
		if (!Files.exists(filePath))
			return;
		try (BufferedReader br = Files.newBufferedReader(filePath)) {
			String line;
			while ((line = br.readLine()) != null) {
				try {
					Highscore h = Highscore.fromString(line);
					scores.add(h);
				} catch (Exception ex) {
					// ignora linhas mal formadas
					System.err.println("Linha highscores ignorada: " + line);
				}
			}
			Collections.sort(scores);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void save() {
		try (BufferedWriter bw = Files.newBufferedWriter(filePath)) {
			for (Highscore h : scores) {
				bw.write(h.toString());
				bw.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}