import Navbar from "./components/Navbar"
import Hero from "./components/Hero"
import Features from "./components/Features"
import Testimonials from "./components/Testimonals";
import CallToAction from "./components/CallToAction"
import Footer from "./components/Footer"

function App() {
    return (
        <div className="min-h-screen flex flex-col">
            <Navbar />
            <main>
                <Hero />
                <Features />
                <Testimonials />
                <CallToAction />
            </main>
            <Footer />
        </div>
    )
}

export default App

