const testimonials = [
    {
        id: 1,
        name: "Sarah Johnson",
        role: "Mathematics Student",
        image: "/placeholder.svg?height=60&width=60",
        quote:
            '"I found an amazing math tutor just 10 minutes away. The in-person sessions have helped me improve my grades significantly."',
    },
    {
        id: 2,
        name: "David Chen",
        role: "Physics Tutor",
        image: "/placeholder.svg?height=60&width=60",
        quote:
            '"The platform makes it easy to manage my schedule and connect with students. The video conferencing tools are top-notch!"',
    },
    {
        id: 3,
        name: "Emma Wilson",
        role: "Parent",
        image: "/placeholder.svg?height=60&width=60",
        quote:
            '"As a parent, I love the safety features and the ability to track my child\'s progress. The payment system is also very convenient."',
    },
]

const Testimonials = () => {
    return (
        <section className="py-16 bg-white">
            <div className="max-w-7xl mx-auto px-6">
                <h2 className="text-3xl font-bold text-center mb-12">Success Stories</h2>

                <div className="grid md:grid-cols-3 gap-8">
                    {testimonials.map((testimonial) => (
                        <div key={testimonial.id} className="bg-white p-8 rounded-lg shadow-md">
                            <div className="flex items-center mb-4">
                                <img
                                    src={testimonial.image || "/placeholder.svg"}
                                    alt={testimonial.name}
                                    className="w-12 h-12 rounded-full mr-4"
                                />
                                <div>
                                    <h3 className="font-bold">{testimonial.name}</h3>
                                    <p className="text-gray-600 text-sm">{testimonial.role}</p>
                                </div>
                            </div>
                            <p className="text-gray-700">{testimonial.quote}</p>
                        </div>
                    ))}
                </div>
            </div>
        </section>
    )
}

export default Testimonials

