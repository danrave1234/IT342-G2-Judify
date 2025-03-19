const HeroImage = () => {
    return (
        <div className="bg-blue-300 rounded-lg p-8 w-full max-w-md">
            <div className="relative">
                <div className="flex justify-center">
                    <div className="relative">
                        {/* Two people looking at laptop */}
                        <div className="flex items-center justify-center">
                            {/* Person 1 */}
                            <div className="mr-2">
                                <div className="w-16 h-16 bg-yellow-300 rounded-full"></div>
                                <div className="w-16 h-20 bg-yellow-200 rounded-b-lg mt-1"></div>
                            </div>

                            {/* Person 2 */}
                            <div className="ml-2">
                                <div className="w-16 h-16 bg-blue-200 rounded-full">
                                    <div className="relative">
                                        <div className="absolute w-10 h-5 bg-white rounded-t-full bottom-0 left-3"></div>
                                        <div className="absolute w-3 h-3 bg-blue-900 rounded-full top-5 left-3"></div>
                                        <div className="absolute w-3 h-3 bg-blue-900 rounded-full top-5 right-3"></div>
                                        <div className="absolute w-8 h-1 bg-blue-900 rounded-full bottom-2 left-4"></div>
                                    </div>
                                </div>
                                <div className="w-16 h-20 bg-blue-100 rounded-b-lg mt-1"></div>
                            </div>
                        </div>

                        {/* Laptop */}
                        <div className="mt-4 mx-auto">
                            <div className="w-48 h-32 bg-blue-200 rounded-t-lg border-2 border-blue-400"></div>
                            <div className="w-48 h-4 bg-blue-400 rounded-b-lg mx-auto"></div>
                            <div className="w-8 h-2 bg-orange-400 rounded-full mx-auto mt-2"></div>
                        </div>

                        {/* Chat bubbles */}
                        <div className="absolute top-0 left-0 w-10 h-10 bg-white rounded-lg -ml-8 -mt-4 flex items-center justify-center">
                            <div className="w-6 h-6 bg-gray-200 rounded-full"></div>
                        </div>
                        <div className="absolute top-0 right-0 w-10 h-10 bg-white rounded-lg -mr-8 -mt-4 flex items-center justify-center">
                            <div className="w-6 h-6 bg-gray-200 rounded-full"></div>
                        </div>
                        <div className="absolute top-12 right-0 w-10 h-10 bg-white rounded-lg -mr-12 flex items-center justify-center">
                            <div className="w-6 h-6 bg-gray-200 rounded-full"></div>
                        </div>
                        <div className="absolute top-12 left-0 w-10 h-10 bg-white rounded-lg -ml-12 flex items-center justify-center">
                            <div className="w-6 h-6 bg-gray-200 rounded-full"></div>
                        </div>
                    </div>
                </div>
                <div className="w-full h-1 bg-blue-400 mt-4"></div>
            </div>
        </div>
    )
}

export default HeroImage

