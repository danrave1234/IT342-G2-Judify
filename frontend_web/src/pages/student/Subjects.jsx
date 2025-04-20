import { useState, useEffect } from 'react';
import { FaSearch, FaFilter, FaBook, FaSort } from 'react-icons/fa';

const Subjects = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const [filteredSubjects, setFilteredSubjects] = useState([]);
  const [subjects, setSubjects] = useState([
    { id: 1, name: 'Mathematics', description: 'Algebra, Calculus, Statistics, and more', tutors: 15, difficulty: 'Intermediate' },
    { id: 2, name: 'Computer Science', description: 'Programming, Algorithms, Data Structures', tutors: 8, difficulty: 'Advanced' },
    { id: 3, name: 'Physics', description: 'Mechanics, Thermodynamics, Electromagnetism', tutors: 10, difficulty: 'Advanced' },
    { id: 4, name: 'English', description: 'Grammar, Literature, Writing', tutors: 12, difficulty: 'Beginner' },
    { id: 5, name: 'Chemistry', description: 'Organic Chemistry, Biochemistry', tutors: 7, difficulty: 'Intermediate' },
    { id: 6, name: 'Biology', description: 'Cellular Biology, Anatomy, Genetics', tutors: 9, difficulty: 'Intermediate' },
    { id: 7, name: 'History', description: 'World History, American History', tutors: 6, difficulty: 'Beginner' },
    { id: 8, name: 'Economics', description: 'Microeconomics, Macroeconomics', tutors: 5, difficulty: 'Intermediate' }
  ]);
  const [selectedDifficulty, setSelectedDifficulty] = useState('All');
  const [sortOption, setSortOption] = useState('name');
  const [showFilters, setShowFilters] = useState(false);

  // Filter and sort subjects
  useEffect(() => {
    let results = [...subjects];
    
    // Apply search filter
    if (searchTerm) {
      results = results.filter(
        subject => subject.name.toLowerCase().includes(searchTerm.toLowerCase()) || 
                   subject.description.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }
    
    // Apply difficulty filter
    if (selectedDifficulty !== 'All') {
      results = results.filter(subject => subject.difficulty === selectedDifficulty);
    }
    
    // Apply sorting
    if (sortOption === 'name') {
      results.sort((a, b) => a.name.localeCompare(b.name));
    } else if (sortOption === 'tutors') {
      results.sort((a, b) => b.tutors - a.tutors);
    } else if (sortOption === 'difficulty') {
      const difficultyOrder = { 'Beginner': 0, 'Intermediate': 1, 'Advanced': 2 };
      results.sort((a, b) => difficultyOrder[a.difficulty] - difficultyOrder[b.difficulty]);
    }
    
    setFilteredSubjects(results);
  }, [searchTerm, subjects, selectedDifficulty, sortOption]);

  return (
    <div className="container mx-auto py-8">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-6">
        <h1 className="text-2xl font-bold">All Subjects</h1>
        <div className="mt-4 md:mt-0 flex items-center space-x-2">
          <button 
            onClick={() => setShowFilters(!showFilters)}
            className="flex items-center px-3 py-2 bg-gray-100 dark:bg-dark-700 rounded-lg text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-dark-600 transition-colors"
          >
            <FaFilter className="mr-2" />
            Filters
          </button>
          <div className="relative">
            <FaSearch className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
            <input
              type="text"
              placeholder="Search subjects..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-10 pr-4 py-2 border border-gray-300 dark:border-dark-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 dark:bg-dark-800 dark:text-white"
            />
          </div>
        </div>
      </div>

      {showFilters && (
        <div className="bg-white dark:bg-dark-800 p-4 rounded-lg shadow-md mb-6 grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Difficulty Level</label>
            <select
              value={selectedDifficulty}
              onChange={(e) => setSelectedDifficulty(e.target.value)}
              className="w-full p-2 border border-gray-300 dark:border-dark-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 dark:bg-dark-800 dark:text-white"
            >
              <option value="All">All Levels</option>
              <option value="Beginner">Beginner</option>
              <option value="Intermediate">Intermediate</option>
              <option value="Advanced">Advanced</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Sort By</label>
            <select
              value={sortOption}
              onChange={(e) => setSortOption(e.target.value)}
              className="w-full p-2 border border-gray-300 dark:border-dark-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 dark:bg-dark-800 dark:text-white"
            >
              <option value="name">Name (A-Z)</option>
              <option value="tutors">Number of Tutors (High to Low)</option>
              <option value="difficulty">Difficulty Level</option>
            </select>
          </div>
        </div>
      )}

      {filteredSubjects.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredSubjects.map(subject => (
            <div key={subject.id} className="bg-white dark:bg-dark-800 rounded-lg shadow-md overflow-hidden hover:shadow-lg transition-shadow">
              <div className="p-6">
                <div className="flex justify-between items-start mb-2">
                  <h2 className="text-xl font-semibold text-gray-900 dark:text-white">{subject.name}</h2>
                  <span className={`px-2 py-1 rounded text-xs font-medium ${
                    subject.difficulty === 'Beginner' ? 'bg-green-100 text-green-800 dark:bg-green-900/20 dark:text-green-400' :
                    subject.difficulty === 'Intermediate' ? 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/20 dark:text-yellow-400' :
                    'bg-red-100 text-red-800 dark:bg-red-900/20 dark:text-red-400'
                  }`}>
                    {subject.difficulty}
                  </span>
                </div>
                <p className="text-gray-600 dark:text-gray-300 mb-4">{subject.description}</p>
                <div className="flex justify-between items-center">
                  <span className="text-sm text-gray-500 dark:text-gray-400">{subject.tutors} tutors available</span>
                  <button className="px-3 py-1 bg-primary-50 text-primary-600 dark:bg-primary-900/20 dark:text-primary-400 rounded-lg hover:bg-primary-100 dark:hover:bg-primary-900/30 transition-colors">
                    View Tutors
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="bg-white dark:bg-dark-800 rounded-lg shadow-md p-8 text-center">
          <FaBook className="text-4xl mx-auto mb-4 text-gray-400 dark:text-gray-500" />
          <h3 className="text-xl font-medium text-gray-900 dark:text-white mb-2">No subjects found</h3>
          <p className="text-gray-500 dark:text-gray-400">
            {searchTerm || selectedDifficulty !== 'All' 
              ? "Try adjusting your search or filters to find more subjects."
              : "No subjects are currently available. Please check back later."}
          </p>
        </div>
      )}
    </div>
  );
};

export default Subjects; 