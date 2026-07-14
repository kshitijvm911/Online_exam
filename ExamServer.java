import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.*;

// ===================================================
// I. OOP CORE DESIGN (Strictly Java Domain)
// ===================================================

class Question {
    private String text;
    private char correctAnswer;

    public Question(String text, char correctAnswer) {
        this.text = text;
        this.correctAnswer = Character.toUpperCase(correctAnswer);
    }
    public String getText() { return text; }
    public boolean checkAnswer(char response) {
        return this.correctAnswer == Character.toUpperCase(response);
    }
}

class NegativeMarkedQuestion extends Question {
    private double penalty;

    public NegativeMarkedQuestion(String text, char correctAnswer, double penalty) {
        super(text, correctAnswer);
        this.penalty = penalty;
    }
    public double getPenalty() { return penalty; }
}

class Student {
    private String id;
    private String name;
    
    public Student(String id, String name) {
        this.id = id;
        this.name = name;
    }
    public String getName() { return name; }
    public String getId() { return id; }
}

class Exam {
    private List<Question> questions = new ArrayList<>();
    private double marksPerQuestion;
    private int durationSeconds; 

    public Exam(double marksPerQuestion, int durationSeconds) {
        this.marksPerQuestion = marksPerQuestion;
        this.durationSeconds = durationSeconds;
    }
    public void addQuestion(Question q) { questions.add(q); }
    public List<Question> getQuestions() { return questions; }
    public double getMarksPerQuestion() { return marksPerQuestion; }
    public int getDurationSeconds() { return durationSeconds; }
}

class ExamResult {
    private Student student;
    private Exam exam;
    private double score;

    public ExamResult(Student student, Exam exam) {
        this.student = student;
        this.exam = exam;
    }

    public void evaluate(String answersCsv) {
        String[] answers = answersCsv.split(",");
        List<Question> questions = exam.getQuestions();
        double total = 0;

        for (int i = 0; i < questions.size(); i++) {
            if (i >= answers.length) break; 
            char studentAns = answers[i].trim().toUpperCase().charAt(0);
            Question q = questions.get(i); 

            if (studentAns == 'S') continue; 

            if (q.checkAnswer(studentAns)) {
                total += exam.getMarksPerQuestion(); 
            } else if (q instanceof NegativeMarkedQuestion) {
                total -= ((NegativeMarkedQuestion) q).getPenalty(); 
            }
        }
        this.score = total;
    }
    public double getScore() { return score; }
    public Student getStudent() { return student; }
}

// ===================================================
// II. SERVER ARCHITECTURE
// ===================================================
public class ExamServer {
    private static long examStartTimeMills; 
    // In-memory Database of valid students
    private static final Map<String, String> studentDatabase = new HashMap<>();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        // Populating our authorized student roster
        studentDatabase.put("STU001", "kshitij Mercer");
        studentDatabase.put("STU002", "Jane Doe");
        studentDatabase.put("STU003", "John Smith");

        Exam javaExam = new Exam(2.0, 10800); 
        
        javaExam.addQuestion(new NegativeMarkedQuestion("Size of int primitive in Java? (A: 2 bytes, B: 4 bytes)", 'B', 1.0));
        javaExam.addQuestion(new NegativeMarkedQuestion("Inheritance keyword? (A: extends, B: imports)", 'A', 1.0));
        javaExam.addQuestion(new NegativeMarkedQuestion("Which component executes Java bytecode? (A: JVM, B: JDK)", 'A', 1.0));
        javaExam.addQuestion(new NegativeMarkedQuestion("Is 'String' a primitive data type in Java? (A: Yes, B: No)", 'B', 1.0));
        javaExam.addQuestion(new NegativeMarkedQuestion("Default value of a boolean primitive variable? (A: true, B: false)", 'B', 1.0));
        javaExam.addQuestion(new NegativeMarkedQuestion("Which keyword prevents a class from being inherited? (A: final, B: static)", 'A', 1.0));
        javaExam.addQuestion(new NegativeMarkedQuestion("Which dynamic collection allows duplicate elements? (A: List, B: Set)", 'A', 1.0));
        javaExam.addQuestion(new NegativeMarkedQuestion("Can an abstract class instantiate objects natively? (A: Yes, B: No)", 'B', 1.0));
        javaExam.addQuestion(new NegativeMarkedQuestion("Which package is imported by default in all Java files? (A: java.util, B: java.lang)", 'B', 1.0));
        javaExam.addQuestion(new NegativeMarkedQuestion("Does Java support direct multiple class inheritance? (A: Yes, B: No)", 'B', 1.0));
        
        examStartTimeMills = System.currentTimeMillis();
        System.out.println("Java Core Engine Active. Authentication systems running.");

        // ROUTE 1: LOGIN VALIDATION ENDPOINT
        server.createContext("/login", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
                
                // Handle the preflight OPTIONS request
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    exchange.close();
                    return;
                }
                
                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    Scanner s = new Scanner(exchange.getRequestBody()).useDelimiter("\\A");
                    String rawPayload = s.hasNext() ? s.next() : "";
                    s.close();

                    // Expecting payload: "id,name"
                    String[] credentials = rawPayload.split(",");
                    String response = "FAIL";

                    if (credentials.length == 2) {
                        String inputId = credentials[0].trim();
                        String inputName = credentials[1].trim();

                        if (studentDatabase.containsKey(inputId) && studentDatabase.get(inputId).equalsIgnoreCase(inputName)) {
                            response = "SUCCESS";
                        }
                    }

                    exchange.sendResponseHeaders(200, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }
            }
        });

        // ROUTE 2: EXAM SUBMISSION ENDPOINT
        server.createContext("/submit", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
                // Whitelist the custom header used by the frontend
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "X-Student-ID"); 
                
                // Handle the preflight OPTIONS request
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    exchange.close();
                    return;
                }
                
                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    long currentTime = System.currentTimeMillis();
                    long timeElapsedSeconds = (currentTime - examStartTimeMills) / 1000;
                    String response;
                    
                    if (timeElapsedSeconds > javaExam.getDurationSeconds()) {
                        response = "REJECTED BY JAVA ENGINE: The 3-Hour Time Limit Exceeded.";
                    } else {
                        // Header contains metadata to safely construct active Student Context
                        String studentId = exchange.getRequestHeaders().getFirst("X-Student-ID");
                        String studentName = studentDatabase.getOrDefault(studentId, "Unknown Student");
                        
                        Scanner s = new Scanner(exchange.getRequestBody()).useDelimiter("\\A");
                        String studentAnswers = s.hasNext() ? s.next() : "";
                        s.close(); 
                        
                        Student activeStudent = new Student(studentId, studentName);
                        ExamResult result = new ExamResult(activeStudent, javaExam);
                        result.evaluate(studentAnswers);
                        
                        response = "Calculated securely inside Java Engine for " + result.getStudent().getName() + " (ID: " + result.getStudent().getId() + "). Final Score: " + result.getScore() + " / 20.0";
                        System.out.println("Submission processed for " + result.getStudent().getName());
                    }
                    
                    exchange.sendResponseHeaders(200, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }
            }
        });

        System.out.println("Server streaming on http://localhost:8080...");
        server.start();
    }
}
