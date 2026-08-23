package com.skillproof.resume;

import com.skillproof.skill.Skill;
import com.skillproof.skill.SkillRepository;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Dictionary-driven skill extraction from free text. Covers all major developer
 * streams (backend, frontend, mobile, AI/ML, data, cloud, DevOps, testing).
 */
@Component
public class SkillCatalog {

    private record Entry(String canonical, String category, List<String> aliases) {}

    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    private static void add(String category, String canonical, String... aliases) {
        ENTRIES.put(canonical, new Entry(canonical, category,
                aliases == null || aliases.length == 0 ? List.of(canonical) : List.of(aliases)));
    }

    static {
        // AI / ML / LLM
        add("AI/ML", "Artificial Intelligence", "artificial intelligence", "ai");
        add("AI/ML", "Machine Learning", "machine learning", "ml");
        add("AI/ML", "Deep Learning", "deep learning");
        add("AI/ML", "Generative AI", "generative ai", "gen ai");
        add("AI/ML", "LLMs", "llm", "llms", "large language model");
        add("AI/ML", "NLP", "nlp", "natural language processing");
        add("AI/ML", "Computer Vision", "computer vision");
        add("AI/ML", "TensorFlow");
        add("AI/ML", "PyTorch");
        add("AI/ML", "scikit-learn", "scikit-learn", "scikitlearn", "sklearn");
        add("AI/ML", "Hugging Face", "hugging face", "huggingface");
        add("AI/ML", "LangChain", "langchain");
        add("AI/ML", "RAG", "rag", "retrieval augmented generation");
        add("AI/ML", "Prompt Engineering", "prompt engineering");
        add("AI/ML", "OpenCV");
        add("AI/ML", "Data Science", "data science");

        // Data engineering & analytics
        add("Data", "Pandas");
        add("Data", "NumPy", "numpy");
        add("Data", "Apache Spark", "apache spark", "spark");
        add("Data", "Hadoop");
        add("Data", "Airflow", "apache airflow");
        add("Data", "dbt");
        add("Data", "Snowflake");
        add("Data", "Databricks");
        add("Data", "ETL", "etl");
        add("Data", "Data Warehouse", "data warehouse", "data warehousing");
        add("Data", "Power BI", "power bi", "powerbi");
        add("Data", "Tableau");
        add("Data", "Elasticsearch");
        add("Data", "Cassandra");
        add("Data", "DynamoDB", "dynamodb");

        // Cloud
        add("Cloud", "AWS");
        add("Cloud", "Azure");
        add("Cloud", "GCP", "gcp", "google cloud");
        add("Cloud", "AWS Lambda", "aws lambda", "lambda functions");
        add("Cloud", "Amazon S3", "amazon s3", "s3 buckets", " s3 ");
        add("Cloud", "EC2", "ec2");
        add("Cloud", "EKS", "eks");
        add("Cloud", "CloudFormation", "cloudformation");
        add("Cloud", "Azure DevOps", "azure devops");
        add("Cloud", "Serverless", "serverless");
        add("Cloud", "Firebase");

        // DevOps
        add("DevOps", "Docker");
        add("DevOps", "Kubernetes", "kubernetes", "k8s");
        add("DevOps", "Terraform");
        add("DevOps", "CI/CD", "ci/cd", "ci cd", "continuous integration");
        add("DevOps", "Jenkins");
        add("DevOps", "Ansible");
        add("DevOps", "Helm", "helm charts");
        add("DevOps", "ArgoCD", "argocd", "argo cd");
        add("DevOps", "GitHub Actions", "github actions");
        add("DevOps", "GitLab CI", "gitlab ci");
        add("DevOps", "Prometheus");
        add("DevOps", "Grafana");
        add("DevOps", "Git");
        add("DevOps", "Linux");

        // Backend & languages
        add("Backend", "Java");
        add("Backend", "Spring Boot");
        add("Backend", "Spring");
        add("Backend", "Python");
        add("Backend", "Go", " go ", "golang");
        add("Backend", "Rust");
        add("Backend", "Kotlin");
        add("Backend", "Scala");
        add("Backend", "PHP");
        add("Backend", "Ruby");
        add("Backend", "Ruby on Rails", "ruby on rails", "rails");
        add("Backend", "C++", "c++");
        add("Backend", "C#", "c#", "csharp");
        add("Backend", ".NET", ".net", "dotnet");
        add("Backend", "Node.js", "node.js", "nodejs", "node js");
        add("Backend", "Express.js", "express.js", "expressjs", "express js");
        add("Backend", "GraphQL");
        add("Backend", "gRPC", "grpc");
        add("Backend", "REST API", "rest api", "restful api", "rest apis");
        add("Backend", "Microservices", "microservices", "microservices architecture");
        add("Backend", "System Design", "system design");
        add("Backend", "DSA", "dsa", "data structures");
        add("Backend", "SQL");
        add("Backend", "Hibernate");
        add("Backend", "JPA", "jpa");
        add("Backend", "Maven");
        add("Backend", "Gradle");
        add("Backend", "Kafka", "apache kafka");
        add("Backend", "RabbitMQ", "rabbitmq");
        add("Backend", "MongoDB", "mongodb");
        add("Backend", "MySQL");
        add("Backend", "Redis");
        add("Backend", "Blockchain");
        add("Backend", "Solidity");

        // Frontend
        add("Frontend", "JavaScript");
        add("Frontend", "TypeScript");
        add("Frontend", "React");
        add("Frontend", "Next.js", "next.js", "nextjs", "next js");
        add("Frontend", "Redux");
        add("Frontend", "Angular");
        add("Frontend", "Vue", "vue.js", "vuejs");
        add("Frontend", "Svelte");
        add("Frontend", "Tailwind CSS", "tailwind css", "tailwindcss", "tailwind");
        add("Frontend", "HTML", "html5");
        add("Frontend", "CSS", "css3");

        // Mobile
        add("Mobile", "Flutter");
        add("Mobile", "React Native", "react native");
        add("Mobile", "Android", "android development", "android sdk");
        add("Mobile", "iOS", "ios development");
        add("Mobile", "Swift");
        add("Mobile", "Dart");

        // Testing
        add("Testing", "JUnit", "junit");
        add("Testing", "Selenium");
        add("Testing", "Cypress");
        add("Testing", "Playwright");
        add("Testing", "Jest");
        add("Testing", "TestNG", "testng");
        add("Testing", "Postman");
    }

    private final SkillRepository skills;

    public SkillCatalog(SkillRepository skills) {
        this.skills = skills;
    }

    /** Canonical skill names found in the text, in dictionary order. */
    public Set<String> detect(String text) {
        String lower = " " + text.toLowerCase(Locale.ROOT) + " ";
        Set<String> found = new LinkedHashSet<>();
        for (Entry e : ENTRIES.values()) {
            for (String alias : e.aliases()) {
                if (containsTerm(lower, alias.toLowerCase(Locale.ROOT))) {
                    found.add(e.canonical());
                    break;
                }
            }
        }
        return found;
    }

    public Optional<Skill> find(String name) {
        return skills.findByNameIgnoreCase(name);
    }

    /** Finds or creates the skill row so any detected stream-specific skill can be claimed. */
    public synchronized Skill findOrCreate(String name, String category) {
        return skills.findByNameIgnoreCase(name).orElseGet(() -> {
            Skill s = new Skill();
            s.setName(name);
            s.setCategory(category == null ? "General" : category);
            return skills.save(s);
        });
    }

    public String categoryOf(String canonicalName) {
        Entry e = ENTRIES.get(canonicalName);
        return e == null ? "General" : e.category();
    }

    /**
     * Boundary-aware containment: "go" must not match inside "google",
     * "ml" not inside "html", but "c++"/"node.js" punctuation is handled.
     */
    static boolean containsTerm(String lowerText, String termLower) {
        int idx = 0;
        while ((idx = lowerText.indexOf(termLower, idx)) >= 0) {
            boolean leftOk = idx == 0 || !Character.isLetterOrDigit(lowerText.charAt(idx - 1));
            int end = idx + termLower.length();
            boolean rightOk = end >= lowerText.length() || !Character.isLetterOrDigit(lowerText.charAt(end));
            if (leftOk && rightOk) return true;
            idx = end;
        }
        return false;
    }
}
