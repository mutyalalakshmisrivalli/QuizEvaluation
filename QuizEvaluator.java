package myPackage;
import java.io.*;
import java.util.*;

interface Evaluator {
    double evaluate(String chosen, String correct, double marks, double penalty);
}

class DefaultEvaluator implements Evaluator {
    @Override
    public double evaluate(String chosen, String correct, double marks, double penalty) {
        if (chosen == null || chosen.isEmpty()) return 0;
        return chosen.equalsIgnoreCase(correct) ? marks : -penalty;
    }
}

abstract class Question {
    protected String id, text, correctOption;
    protected double marks, penalty;

    public Question(String id, String text, double marks, double penalty, String correctOption) {
        this.id = id; this.text = text;
        this.marks = marks; this.penalty = penalty;
        this.correctOption = correctOption;
    }

    public String getId() { return id; }
    public String getCorrectOption() { return correctOption; }
    public double getMarks() { return marks; }
    public double getPenalty() { return penalty; }
}

class MCQ extends Question {
    List<String> options;
    public MCQ(String id, String text, List<String> options,
            String correctOption, double marks, double penalty) {
        super(id, text, marks, penalty, correctOption);
        this.options = options;
    }
}

public class QuizEvaluator {

    private static final Scanner sc = new Scanner(System.in);

    private static Map<String, Question> questions = new LinkedHashMap<>();
    private static Map<String, String> answers = new LinkedHashMap<>();
    private static Map<String, Map<String, String>> responses = new LinkedHashMap<>();
    private static Evaluator evaluator = new DefaultEvaluator();

    public static void main(String[] args) {
        int choice;
        do {
            printMenu();
            choice = readInt();

            switch (choice) {
                case 1 -> loadData();
                case 2 -> generateScoreReport();
                case 3 -> showQuestions();
                case 4 -> showAnswers();
                case 5 -> exportDifficultyAnalysis();
                case 6 -> exportDetailedReport();
                case 7 -> System.out.println("Exiting...");
                default -> System.out.println("Invalid choice!");
            }
        } while (choice != 7);
    }

    private static void printMenu() {
        System.out.println("\n===== QUIZ SYSTEM =====");
        System.out.println("1. Load Data Files");
        System.out.println("2. Generate Score Report");
        System.out.println("3. Show Questions");
        System.out.println("4. Show Answers");
        System.out.println("5. Export Difficulty Analysis");
        System.out.println("6. Export Detailed Student Report");
        System.out.println("7. Exit");
        System.out.print("Choose: ");
    }

    private static int readInt() {
        try { return Integer.parseInt(sc.nextLine()); }
        catch (Exception e) { return -1; }
    }

    private static void loadData() {
        loadQuestions();
        loadAnswers();
        loadResponses();
    }

    private static void loadQuestions() {
        System.out.print("Path to questions.csv: ");
        String path = sc.nextLine();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {

            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");

                Question q = new MCQ(
                        p[0], p[1],
                        Arrays.asList(p[2].split("\\|")),
                        p[3],
                        Double.parseDouble(p[4]),
                        Double.parseDouble(p[5])
                );
                questions.put(p[0], q);
            }
            System.out.println("Questions loaded: " + questions.size());

        } catch (Exception e) {
            System.out.println("Error loading questions: " + e);
        }
    }

    private static void loadAnswers() {
        System.out.print("Path to answers.csv: ");
        String path = sc.nextLine();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {

            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                answers.put(p[0], p[1]);
            }
            System.out.println("Answers loaded: " + answers.size());

        } catch (Exception e) {
            System.out.println("Error loading answers: " + e);
        }
    }

    private static void loadResponses() {
        System.out.print("Path to responses.csv: ");
        String path = sc.nextLine();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {

            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                responses.putIfAbsent(p[0], new HashMap<>());
                responses.get(p[0]).put(p[1], p[2]);
            }
            System.out.println("Responses loaded: " + responses.size());

        } catch (Exception e) {
            System.out.println("Error loading responses: " + e);
        }
    }

    private static void generateScoreReport() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("report.csv"))) {
            bw.write("student,score\n");

            for (String student : responses.keySet()) {
                double total = 0;

                for (String qid : questions.keySet()) {
                    Question q = questions.get(qid);
                    String chosen = responses.get(student).get(qid);
                    total += evaluator.evaluate(chosen, q.getCorrectOption(), q.getMarks(), q.getPenalty());
                }

                bw.write(student + "," + total + "\n");
            }

            System.out.println("Saved: report.csv");
        } catch (Exception e) {
            System.out.println("Error writing report: " + e);
        }
    }

    private static void showQuestions() {
        System.out.println("\n=== QUESTIONS ===");
        questions.forEach((id, q) -> System.out.println(id + ": " + q.text));
    }

    private static void showAnswers() {
        System.out.println("\n=== ANSWERS ===");
        answers.forEach((k, v) -> System.out.println(k + ": " + v));
    }

    private static void exportDifficultyAnalysis() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("difficulty.csv"))) {
            bw.write("qid,correct,total,difficulty\n");

            for (String qid : questions.keySet()) {
                int correct = 0, total = 0;

                for (String student : responses.keySet()) {
                    total++;
                    String chosen = responses.get(student).get(qid);
                    if (answers.get(qid).equalsIgnoreCase(chosen)) correct++;
                }

                double diff = 1 - (double) correct / total;
                bw.write(qid + "," + correct + "," + total + "," + diff + "\n");
            }

            System.out.println("Saved: difficulty.csv");
        } catch (Exception e) {
            System.out.println("Error difficulty: " + e);
        }
    }

    private static void exportDetailedReport() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("detailed_report.csv"))) {
            bw.write("student,qid,chosen,correct,marks\n");

            for (String student : responses.keySet()) {
                for (String qid : questions.keySet()) {
                    Question q = questions.get(qid);
                    String chosen = responses.get(student).get(qid);
                    double score = evaluator.evaluate(chosen, q.getCorrectOption(), q.getMarks(), q.getPenalty());

                    bw.write(student + "," + qid + "," + chosen + "," +
                            q.getCorrectOption() + "," + score + "\n");
                }
            }

            System.out.println("Saved: detailed_report.csv");
        } catch (Exception e) {
            System.out.println("Error detailed: " + e);
        }
    }
}