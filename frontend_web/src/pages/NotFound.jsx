import { Link } from 'react-router-dom';

const NotFound = () => {
  return (
    <div className="page-container flex flex-col items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
      <div className="text-center">
        <h1 className="text-6xl font-bold text-primary-600 dark:text-primary-500">404</h1>
        <h2 className="mt-2 text-3xl font-bold text-gray-900 dark:text-white">Page Not Found</h2>
        <p className="mt-4 text-lg text-gray-600 dark:text-gray-400">
          Sorry, we couldn't find the page you're looking for.
        </p>
        <div className="mt-8">
          <Link
            to="/"
            className="btn-primary"
          >
            Go back home
          </Link>
        </div>
      </div>
    </div>
  );
};

export default NotFound; 