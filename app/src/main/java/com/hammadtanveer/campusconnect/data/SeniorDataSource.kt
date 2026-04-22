package com.hammadtanveer.campusconnect.data

import com.hammadtanveer.campusconnect.data.models.SeniorProfile

object SeniorDataSource {

    val seniors: List<SeniorProfile> = listOf(
        SeniorProfile(
            id = "1",
            name = "Hammad Tanveer",
            branch = "Computer Science",
            batch = "4th Year",
            mobileNumber = "",
            profileImageUrl = "",
            linkedinUrl = "https://www.linkedin.com/in/hammad-tanveer",
            bio = "Expertise: Android Developer Full Stack. \n\nBio: Experienced Android engineer with full-stack experience; focused on building scalable mobile apps and mentoring juniors on end-to-end development.\n\nTech Stack: Kotlin, Java, Jetpack Compose, Android Architecture, Retrofit, Room, Firebase, GraphQL, Node.js, Express, MongoDB"
        ),
        SeniorProfile(
            id = "2",
            name = "Mohammad Adnan",
            branch = "Computer Science",
            batch = "4th Year",
            mobileNumber = "",
            profileImageUrl = "",
            linkedinUrl = "https://www.linkedin.com/in/mohammad-adnan",
            bio = "Expertise: AI/ML. \n\nBio: AI/ML engineer who enjoys turning models into products; offers guidance on model selection, training and deployment for mentees.\n\nTech Stack: Python, TensorFlow, PyTorch, scikit-learn, NumPy, Pandas, Hugging Face Transformers, MLflow, Docker, Flask/FastAPI"
        ),
        SeniorProfile(
            id = "3",
            name = "Mohd Arham",
            branch = "Computer Science",
            batch = "4th Year",
            mobileNumber = "",
            profileImageUrl = "",
            linkedinUrl = "https://www.linkedin.com/in/mohd-arham",
            bio = "Expertise: Testing and UX. \n\nBio: Quality-focused engineer specializing in testing strategies and user-centered design; mentors on test automation and UX improvements.\n\nTech Stack: Espresso, JUnit, Mockito, Robolectric, UI/UX Principles, Accessibility Testing, Figma, Usability Testing, TestRail, Postman"
        ),
        SeniorProfile(
            id = "4",
            name = "Mohd Faisal",
            branch = "Computer Science",
            batch = "4th Year",
            mobileNumber = "",
            profileImageUrl = "",
            linkedinUrl = "https://www.linkedin.com/in/mohd-faisal",
            bio = "Expertise: Android Development UI. \n\nBio: UI-focused Android developer passionate about delightful interfaces and accessibility; helps mentees design and implement modern UIs.\n\nTech Stack: Kotlin, Jetpack Compose, XML Layouts, Material Design, ConstraintLayout, MotionLayout, Lottie, Accessible Design, Figma, Android Studio"
        ),
        SeniorProfile(
            id = "5",
            name = "Mohd Arman",
            branch = "Computer Science",
            batch = "4th Year",
            mobileNumber = "",
            profileImageUrl = "",
            linkedinUrl = "https://www.linkedin.com/in/mohd-arman",
            bio = "Expertise: Full Stack Web App using React. \n\nBio: Full-stack web engineer focused on React-driven interfaces and backend services; mentors on building production-ready web apps.\n\nTech Stack: JavaScript, TypeScript, React, Redux, Next.js, Node.js, Express, PostgreSQL, GraphQL, Docker, AWS"
        )
    )

    fun getSeniorById(id: String): SeniorProfile? = seniors.find { it.id == id }
}

