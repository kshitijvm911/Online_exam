import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.*;

// ===================================================
// I. OOP CORE DESIGN 
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
        String[] answers = answersCsv.split(",", -1);
        List<Question> questions = exam.getQuestions();
        double total = 0;

        for (int i = 0; i < questions.size(); i++) {
            if (i >= answers.length) break; 
            String ansStr = answers[i].trim();
            if (ansStr.isEmpty()) continue;
            
            char studentAns = ansStr.toUpperCase().charAt(0);
            Question q = questions.get(i); 

            if (studentAns == 'S') continue; // Skipped / Unanswered question

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
    private static final Map<String, String> studentDatabase = new HashMap<>();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        // Registered Student Database
        studentDatabase.put("STU001", "kshitij");
        studentDatabase.put("STU002", "ashin");
        studentDatabase.put("STU003", "shaza");
        studentDatabase.put("STU004", "avani");

        // Initialize exams for three subjects
        Exam javaExam = new Exam(2.0, 10800); 
        Exam cppExam = new Exam(2.0, 10800);
        Exam cExam = new Exam(2.0, 10800);

        // JAVA Questions Configuration
        javaExam.addQuestion(new NegativeMarkedQuestion("What is the size of primitive int data type in java?", 'B', 1.0));
        javaExam.addQuestion(new NegativeMarkedQuestion("Inheritance keyword?", 'A', 1.0));
        javaExam.addQuestion(new NegativeMarkedQuestion("Which component executes Java bytecode?", 'A', 1.0));
        javaExam.addQuestion(new NegativeMarkedQuestion("Is 'String' a primitive data type in Java?", 'B', 1.0));
        javaExam.addQuestion(new NegativeMarkedQuestion("Default value of a boolean primitive variable?", 'B', 1.0));
        javaExam.addQuestion(new NegativeMarkedQuestion("Which keyword prevents a class from being inherited?", 'A', 1.0));
        javaExam.addQuestion(new NegativeMarkedQuestion("Which dynamic collection allows duplicate elements?", 'A', 1.0));
        javaExam.addQuestion(new NegativeMarkedQuestion("Can an abstract class instantiate objects natively?", 'B', 1.0));
        javaExam.addQuestion(new NegativeMarkedQuestion("Which package is imported by default in all Java files?", 'B', 1.0));
        javaExam.addQuestion(new NegativeMarkedQuestion("Does Java support direct multiple class inheritance?", 'B', 1.0));

        // C++ Questions Configuration
        cppExam.addQuestion(new NegativeMarkedQuestion("Who created C++?", 'A', 1.0));
        cppExam.addQuestion(new NegativeMarkedQuestion("What is the output operator in C++?", 'A', 1.0));
        cppExam.addQuestion(new NegativeMarkedQuestion("How do you deallocate memory assigned by 'new'?", 'B', 1.0));
        cppExam.addQuestion(new NegativeMarkedQuestion("What is the default access specifier in a C++ class?", 'B', 1.0));
        cppExam.addQuestion(new NegativeMarkedQuestion("Which keyword is used to group logical code blocks?", 'B', 1.0));
        cppExam.addQuestion(new NegativeMarkedQuestion("Can C++ destructors take arguments?", 'B', 1.0));
        cppExam.addQuestion(new NegativeMarkedQuestion("Does C++ support multiple inheritance?", 'A', 1.0));
        cppExam.addQuestion(new NegativeMarkedQuestion("Which keyword denotes an inline function?", 'A', 1.0));
        cppExam.addQuestion(new NegativeMarkedQuestion("What does STL stand for?", 'A', 1.0));
        cppExam.addQuestion(new NegativeMarkedQuestion("Is 'cin' used for input or output?", 'A', 1.0));

        // C Questions Configuration
        cExam.addQuestion(new NegativeMarkedQuestion("What is the format specifier for an integer in C?", 'A', 1.0));
        cExam.addQuestion(new NegativeMarkedQuestion("Which function is used for dynamic memory allocation?", 'A', 1.0));
        cExam.addQuestion(new NegativeMarkedQuestion("Does C support Object-Oriented Programming?", 'B', 1.0));
        cExam.addQuestion(new NegativeMarkedQuestion("What symbol is used for the address-of operator?", 'B', 1.0));
        cExam.addQuestion(new NegativeMarkedQuestion("What is the null character string terminator?", 'A', 1.0));
        cExam.addQuestion(new NegativeMarkedQuestion("Which keyword exits a loop early?", 'B', 1.0));
        cExam.addQuestion(new NegativeMarkedQuestion("Does C have a built-in boolean data type in C89?", 'B', 1.0));
        cExam.addQuestion(new NegativeMarkedQuestion("Who developed the C language?", 'A', 1.0));
        cExam.addQuestion(new NegativeMarkedQuestion("Is an array index 0-based in C?", 'A', 1.0));
        cExam.addQuestion(new NegativeMarkedQuestion("Which header file is required for printf?", 'B', 1.0));
        
        examStartTimeMills = System.currentTimeMillis();

        // LOGIN ENDPOINT
        server.createContext("/login", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
                
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    exchange.close();
                    return;
                }
                
                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    Scanner s = new Scanner(exchange.getRequestBody()).useDelimiter("\\A");
                    String rawPayload = s.hasNext() ? s.next() : "";
                    s.close();

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

        // MULTI-SUBJECT SUBMISSION ENDPOINT
        server.createContext("/submit", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "X-Student-ID"); 
                
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    exchange.close();
                    return;
                }
                
                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    // Uses examStartTimeMills to satisfy compiler/IDE warning checks
                    long elapsedSec = (System.currentTimeMillis() - examStartTimeMills) / 1000;
                    String studentId = exchange.getRequestHeaders().getFirst("X-Student-ID");
                    String studentName = studentDatabase.getOrDefault(studentId, "Unknown Student");
                    
                    Scanner s = new Scanner(exchange.getRequestBody()).useDelimiter("\\A");
                    String rawPayload = s.hasNext() ? s.next() : "";
                    s.close(); 
                    
                    Student activeStudent = new Student(studentId, studentName);
                    
                    double javaScore = 0.0, cppScore = 0.0, cScore = 0.0;
                    
                    // Format: Java=A,S,B...|C++=A,B...|C=B...
                    String[] subjectPayloads = rawPayload.split("\\|");
                    for (String subPayload : subjectPayloads) {
                        String[] parts = subPayload.split("=");
                        if (parts.length == 2) {
                            String subjectName = parts[0].trim();
                            String csvAnswers = parts[1].trim();
                            
                            if (subjectName.equalsIgnoreCase("Java")) {
                                ExamResult r = new ExamResult(activeStudent, javaExam);
                                r.evaluate(csvAnswers);
                                javaScore = r.getScore();
                            } else if (subjectName.equalsIgnoreCase("C++")) {
                                ExamResult r = new ExamResult(activeStudent, cppExam);
                                r.evaluate(csvAnswers);
                                cppScore = r.getScore();
                            } else if (subjectName.equalsIgnoreCase("C")) {
                                ExamResult r = new ExamResult(activeStudent, cExam);
                                r.evaluate(csvAnswers);
                                cScore = r.getScore();
                            }
                        }
                    }
                    
                    double totalScore = javaScore + cppScore + cScore;
                    
                    String response = String.format("Java:%.1f, C++:%.1f, C:%.1f, Total:%.1f", 
                                             javaScore, cppScore, cScore, totalScore);
                    
                    System.out.println("Submission received at " + elapsedSec + "s for " + studentName + ". Total Marks: " + totalScore);

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
