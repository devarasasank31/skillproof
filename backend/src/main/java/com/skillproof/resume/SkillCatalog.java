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
 * Dictionary-driven skill extraction from free text. Covers every common resume
 * stream: engineering (backend, frontend, mobile, AI/ML, data, cloud, DevOps,
 * testing, security) as well as design/UI-UX, marketing, business, finance and HR.
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

        // UI / UX / Design
        add("Design", "UI Design", "ui design", "ui developer", "user interface design");
        add("Design", "UX Design", "ux design", "ux developer", "user experience design", "user experience");
        add("Design", "UI/UX", "ui/ux", "ui ux", "ui & ux");
        add("Design", "Product Design", "product designer", "product designing");
        add("Design", "Graphic Design", "graphic designer", "graphic designing");
        add("Design", "Web Design", "web designer", "web designing");
        add("Design", "Figma");
        add("Design", "Adobe XD", "adobe xd", "adobexd");
        add("Design", "Sketch", "sketch app");
        add("Design", "InVision", "invision");
        add("Design", "Framer", "framer motion", "framer");
        add("Design", "Zeplin");
        add("Design", "Axure");
        add("Design", "Balsamiq");
        add("Design", "Marvel App", "marvel app", "marvel prototyping");
        add("Design", "Wireframing", "wireframe", "wireframes", "wireframing");
        add("Design", "Prototyping", "prototype", "prototypes", "prototyping");
        add("Design", "User Research", "user research", "user interviews", "user survey");
        add("Design", "Usability Testing", "usability testing", "usability study");
        add("Design", "Personas", "persona development", "user personas");
        add("Design", "User Journeys", "user journey", "journey mapping", "customer journey");
        add("Design", "Information Architecture", "information architecture");
        add("Design", "Interaction Design", "interaction design", "interaction designer");
        add("Design", "Design Systems", "design system", "design guidelines");
        add("Design", "Typography", "typography");
        add("Design", "Color Theory", "color theory", "colour theory");
        add("Design", "Accessibility", "accessibility", "wcag", "a11y");
        add("Design", "Responsive Design", "responsive design", "responsive web design");
        add("Design", "Motion Design", "motion design", "motion graphics");
        add("Design", "Adobe Photoshop", "photoshop", "adobe photoshop");
        add("Design", "Adobe Illustrator", "illustrator", "adobe illustrator");
        add("Design", "After Effects", "after effects", "adobe after effects");
        add("Design", "Premiere Pro", "premiere pro", "adobe premiere");
        add("Design", "Canva");
        add("Design", "Blender", "blender 3d");
        add("Design", "3D Modeling", "3d modeling", "3d modelling", "3ds max");
        add("Design", "Adobe Creative Suite", "adobe creative suite", "creative cloud");

        // Marketing / Content
        add("Marketing", "Digital Marketing", "digital marketer", "digital marketing");
        add("Marketing", "SEO", "seo", "search engine optimization");
        add("Marketing", "SEM", "sem", "search engine marketing");
        add("Marketing", "Google Ads", "google ads", "google adwords", "adwords");
        add("Marketing", "Social Media Marketing", "social media marketing", "smm", "social media manager");
        add("Marketing", "Content Marketing", "content marketing", "content strategist");
        add("Marketing", "Copywriting", "copywriting", "copywriter");
        add("Marketing", "Content Writing", "content writer", "content writing", "technical writing", "blogger");
        add("Marketing", "Email Marketing", "email marketing", "mailchimp");
        add("Marketing", "Google Analytics", "google analytics", "ga4");
        add("Marketing", "HubSpot", "hubspot");
        add("Marketing", "Brand Management", "brand management", "branding");
        add("Marketing", "Market Research", "market research");
        add("Marketing", "Campaign Management", "campaign management", "marketing campaigns");
        add("Marketing", "Growth Marketing", "growth hacking", "growth marketing");

        // Business / Management / Finance / HR
        add("Business", "Project Management", "project management", "project manager", "pmp");
        add("Business", "Agile", "agile methodology", "agile");
        add("Business", "Scrum", "scrum master", "scrum");
        add("Business", "Jira");
        add("Business", "Confluence");
        add("Business", "Product Management", "product manager", "product management");
        add("Business", "Business Analysis", "business analyst", "business analysis");
        add("Business", "Stakeholder Management", "stakeholder management");
        add("Business", "Financial Analysis", "financial analysis", "financial analyst");
        add("Business", "Accounting", "accountant", "accounting");
        add("Business", "Microsoft Excel", "ms excel", "excel", "advanced excel");
        add("Business", "Tally", "tally erp");
        add("Business", "SAP", "sap erp", "sap mm", "sap fico");
        add("Business", "Budgeting", "budgeting", "budget planning");
        add("Business", "Forecasting", "forecasting", "financial modeling");
        add("Business", "Risk Management", "risk management");
        add("Business", "Salesforce", "salesforce crm", "salesforce");
        add("Business", "CRM", "crm", "customer relationship management");
        add("Business", "Sales", "sales executive", "b2b sales", "inside sales");
        add("Business", "Lead Generation", "lead generation");
        add("Business", "Negotiation", "negotiation");
        add("Business", "Customer Success", "customer success", "client servicing");
        add("Business", "Operations Management", "operations management", "operations");
        add("Business", "Supply Chain", "supply chain", "logistics");
        add("Business", "Six Sigma", "six sigma", "lean six sigma");
        add("Business", "Recruitment", "recruitment", "technical recruiter");
        add("Business", "Talent Acquisition", "talent acquisition");
        add("Business", "Human Resources", "human resources", "hr manager", "hr");
        add("Business", "Public Speaking", "public speaking");
        add("Business", "Team Leadership", "team leadership", "team lead", "people management");

        // Cybersecurity
        add("Security", "Penetration Testing", "penetration testing", "pen testing", "vapt");
        add("Security", "Ethical Hacking", "ethical hacking");
        add("Security", "Network Security", "network security");
        add("Security", "OWASP", "owasp");
        add("Security", "Burp Suite", "burp suite");
        add("Security", "Wireshark");
        add("Security", "Cryptography", "cryptography", "encryption");
        add("Security", "SIEM", "siem");
        add("Security", "Information Security", "information security", "infosec");

        // Other popular tools / domains
        add("Tools", "WordPress", "wordpress");
        add("Tools", "Shopify", "shopify");
        add("Tools", "Unity", "unity 3d", "unity engine");
        add("Business", "Unreal Engine", "unreal engine");
        add("Tools", "MATLAB");
        add("Tools", "AutoCAD", "autocad");
        add("Tools", "SolidWorks", "solidworks");
        add("Tools", "Miro");
        add("Tools", "Notion");
        add("Tools", "Slack");
        add("Data", "Looker Studio", "looker studio", "data studio");
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
