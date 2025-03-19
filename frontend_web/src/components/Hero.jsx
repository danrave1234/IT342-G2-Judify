import HeroImage from "../assets/hero-image.jsx"

const Hero = () => {
    return (
        <section className="bg-blue-50 py-16">
            <div className="max-w-7xl mx-auto px-6 grid md:grid-cols-2 gap-12 items-center">
                <div>
                    <h1 className="text-4xl md:text-5xl font-bold text-gray-900 mb-4">
                        Find Your Perfect Tutor, Online or Nearby
                    </h1>
                    <p className="text-gray-600 mb-8">
                        Connect with expert tutors for in-person or virtual sessions. Smart matching based on location, expertise,
                        and availability.
                    </p>
                    <div className="flex flex-wrap gap-4">
                        <a href="#find-tutor" className="bg-blue-600 text-white px-6 py-3 rounded-full hover:bg-blue-700">
                            Find a Tutor
                        </a>
                        <a
                            href="#become-tutor"
                            className="bg-white text-blue-600 border border-blue-600 px-6 py-3 rounded-full hover:bg-blue-50"
                        >
                            Become a Tutor
                        </a>
                    </div>
                </div>
                <div className="flex justify-center">
                    <HeroImage />
                </div>
            </div>
        </section>
    )
}

export default Hero

