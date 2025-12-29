package com.achat.history;

public class UserStats {
    private int totalGames;
    private int wins;
    private int losses;
    private int draws;
    private double winRate;
    private double avgScore;
    private int bestScore;
    private int currentStreak;

    public UserStats() {
        this.totalGames = 0;
        this.wins = 0;
        this.losses = 0;
        this.draws = 0;
        this.winRate = 0.0;
        this.avgScore = 0.0;
        this.bestScore = 0;
        this.currentStreak = 0;
    }

    // Getters
    public int getTotalGames() { return totalGames; }
    public int getWins() { return wins; }
    public int getLosses() { return losses; }
    public int getDraws() { return draws; }
    public double getWinRate() { return winRate; }
    public double getAvgScore() { return avgScore; }
    public int getBestScore() { return bestScore; }
    public int getCurrentStreak() { return currentStreak; }

    // Setters
    public void setTotalGames(int totalGames) { this.totalGames = totalGames; }
    public void setWins(int wins) { this.wins = wins; }
    public void setLosses(int losses) { this.losses = losses; }
    public void setDraws(int draws) { this.draws = draws; }
    public void setWinRate(double winRate) { this.winRate = winRate; }
    public void setAvgScore(double avgScore) { this.avgScore = avgScore; }
    public void setBestScore(int bestScore) { this.bestScore = bestScore; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }
}