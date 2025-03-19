const CallToAction = () => {
    return (
        <section className="bg-blue-600 py-16 text-white">
            <div className="max-w-3xl mx-auto px-6 text-center">
                <h2 className="text-3xl font-bold mb-4">Ready to Start Learning?</h2>
                <p className="mb-8">Join thousands of students who are already improving their grades with Judify</p>
                <div className="flex flex-wrap justify-center gap-4">
                    <a href="#get-started" className="bg-white text-blue-600 px-6 py-3 rounded-full hover:bg-gray-100">
                        Get Started Now
                    </a>
                    <a
                        href="#learn-more"
                        className="bg-transparent border border-white text-white px-6 py-3 rounded-full hover:bg-blue-700"
                    >
                        Learn More
                    </a>
                </div>
            </div>
        </section>
    )
}

export default CallToAction

