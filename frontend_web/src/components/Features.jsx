import { MapPin, Video, CreditCard } from "lucide-react"

const Features = () => {
    return (
        <section className="py-16">
            <div className="max-w-7xl mx-auto px-6">
                <h2 className="text-3xl font-bold text-center mb-12">Why Choose Judify?</h2>

                <div className="grid md:grid-cols-3 gap-8">
                    {/* Feature 1 */}
                    <div className="bg-white p-8 rounded-lg shadow-md">
                        <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center mb-4">
                            <MapPin className="text-blue-600" size={24} />
                        </div>
                        <h3 className="text-xl font-bold mb-3">Smart Location Matching</h3>
                        <p className="text-gray-600">
                            Find tutors near you or connect remotely. GPS-based matching for in-person sessions.
                        </p>
                    </div>

                    {/* Feature 2 */}
                    <div className="bg-white p-8 rounded-lg shadow-md">
                        <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center mb-4">
                            <Video className="text-blue-600" size={24} />
                        </div>
                        <h3 className="text-xl font-bold mb-3">Virtual Sessions</h3>
                        <p className="text-gray-600">
                            High-quality video conferencing with document sharing and interactive tools.
                        </p>
                    </div>

                    {/* Feature 3 */}
                    <div className="bg-white p-8 rounded-lg shadow-md">
                        <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center mb-4">
                            <CreditCard className="text-blue-600" size={24} />
                        </div>
                        <h3 className="text-xl font-bold mb-3">Secure Payments</h3>
                        <p className="text-gray-600">Automated billing and secure payment processing for all sessions.</p>
                    </div>
                </div>
            </div>
        </section>
    )
}

export default Features

