package com.example.campusconnect.data

object SeniorDataSource {

    val seniors: List<Senior> = listOf(
        Senior(
            id = 1,
            name = "Hammad Tanveer",
            expertise = "Android Developer Full Stack",
            bio = "Experienced Android engineer with full-stack experience; focused on building scalable mobile apps and mentoring juniors on end-to-end development.",
            technicalStack = listOf(
                "Kotlin",
                "Java",
                "Jetpack Compose",
                "Android Architecture (ViewModel, LiveData)",
                "Retrofit",
                "Room",
                "Firebase (Auth, Firestore)",
                "GraphQL",
                "Node.js",
                "Express",
                "MongoDB"
            ),
            availability = "Weekday evenings (India Time) - flexible for mentoring calls",
            contact = ContactInfo(
                email = "hammad.tanveer@example.com",
                linkedin = "https://www.linkedin.com/in/hammad-tanveer",
                github = "https://github.com/hammadtanveer"
            )
        ),

        Senior(
            id = 2,
            name = "Mohammad Adnan",
            expertise = "AI/ML",
            bio = "AI/ML engineer who enjoys turning models into products; offers guidance on model selection, training and deployment for mentees.",
            technicalStack = listOf(
                "Python",
                "TensorFlow",
                "PyTorch",
                "scikit-learn",
                "NumPy",
                "Pandas",
                "Hugging Face Transformers",
                "MLflow",
                "Docker",
                "Flask/FastAPI"
            ),
            availability = "Weekends and weekday mornings (by appointment)",
            contact = ContactInfo(
                email = "mohammad.adnan@example.com",
                linkedin = "https://www.linkedin.com/in/mohammad-adnan",
                github = "https://github.com/mohammadadnan"
            )
        ),

        Senior(
            id = 3,
            name = "Mohd Arham",
            expertise = "Testing and UX",
            bio = "Quality-focused engineer specializing in testing strategies and user-centered design; mentors on test automation and UX improvements.",
            technicalStack = listOf(
                "Espresso",
                "JUnit",
                "Mockito",
                "Robolectric",
                "UI/UX Principles",
                "Accessibility Testing",
                "Figma",
                "Usability Testing",
                "TestRail",
                "Postman"
            ),
            availability = "Flexible — prefers short weekly sessions",
            contact = ContactInfo(
                email = "mohd.arham@example.com",
                linkedin = "https://www.linkedin.com/in/mohd-arham",
                github = "https://github.com/mohdarham"
            )
        ),

        Senior(
            id = 4,
            name = "Mohd Faisal",
            expertise = "Android Development UI",
            bio = "UI-focused Android developer passionate about delightful interfaces and accessibility; helps mentees design and implement modern UIs.",
            technicalStack = listOf(
                "Kotlin",
                "Jetpack Compose",
                "XML Layouts",
                "Material Design",
                "ConstraintLayout",
                "MotionLayout",
                "Lottie",
                "Accessible Design",
                "Figma",
                "Android Studio"
            ),
            availability = "Weekday afternoons (IST) — booking recommended",
            contact = ContactInfo(
                email = "mohd.faisal@example.com",
                linkedin = "https://www.linkedin.com/in/mohd-faisal",
                github = "https://github.com/mohdfaisal"
            )
        ),

        Senior(
            id = 5,
            name = "Mohd Arman",
            expertise = "Full Stack Web App using React",
            bio = "Full-stack web engineer focused on React-driven interfaces and backend services; mentors on building production-ready web apps.",
            technicalStack = listOf(
                "JavaScript",
                "TypeScript",
                "React",
                "Redux",
                "Next.js",
                "Node.js",
                "Express",
                "PostgreSQL",
                "GraphQL",
                "Docker",
                "AWS"
            ),
            availability = "Evenings and weekends — open to code reviews",
            contact = ContactInfo(
                email = "mohd.arman@example.com",
                linkedin = "https://www.linkedin.com/in/mohd-arman",
                github = "https://github.com/mohdarman"
            )
        )
    )

    fun getSeniorById(id: Int): Senior? = seniors.find { it.id == id }
}

