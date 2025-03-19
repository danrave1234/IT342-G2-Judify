const Navbar = () => {
    return (
        <nav className="bg-white py-4 px-6 shadow-sm">
            <div className="max-w-7xl mx-auto flex items-center justify-between">
                <div className="text-2xl font-bold text-blue-600">Judify</div>

                <div className="hidden md:flex items-center space-x-8">
                    <a href="#how-it-works" className="text-gray-600 hover:text-blue-600">
                        How it Works
                    </a>
                    <a href="#find-tutors" className="text-gray-600 hover:text-blue-600">
                        Find Tutors
                    </a>
                    <a href="#become-tutor" className="text-gray-600 hover:text-blue-600">
                        Become a Tutor
                    </a>
                    <a href="#pricing" className="text-gray-600 hover:text-blue-600">
                        Pricing
                    </a>
                </div>

                <div className="flex items-center space-x-4">
                    <a href="#login" className="text-blue-600 hover:text-blue-800">
                        Login
                    </a>
                    <a href="#signup" className="bg-blue-600 text-white px-4 py-2 rounded-full hover:bg-blue-700">
                        Sign Up
                    </a>
                </div>
            </div>
        </nav>
    )
}

export default Navbar

