import { BrowserRouter as Router, Route, Routes, Navigate } from "react-router";
import LoginPage from "./pages/Auth/LoginPage.jsx";
import RegisterPage from "./pages/Auth/RegisterPage.jsx";
import NotFoundPage from "./pages/NotFoundPage.jsx";
import ProtectedRoute from "./Components/auth/ProtectedRoute.jsx";
import DashboardPage from "./pages/Dashboard/DashboardPage.jsx";
import DocumentListPage from "./pages/Documents/DocumentListPage.jsx";
import DocumentDetailPage from "./pages/Documents/DocumentDetailPage.jsx";
import FlashcardsListPage from "./pages/Flashcards/FlashcardListPage.jsx";
import FlashcardPage from "./pages/Flashcards/FlashcardPage.jsx";
import QuizTakePage from "./pages/Quizzes/QuizTakePage.jsx";
import QuizResultPage from "./pages/Quizzes/QuizResultPage.jsx";
import ProfilePage from "./pages/Profile/ProfilePage.jsx";
import { useAuth } from './context/AuthContext.jsx';


const App = () => {
  const {isAuthenticated, loading} = useAuth();

  if (loading) {
    return (
      <div className="flex items-center justify-center h-screen">
        <p>Loading...</p>
      </div>
    )
  }
  return (
      <Router>
        <Routes>
          <Route path="/" element={isAuthenticated ? (<Navigate to="/dashboard" replace />) : (<Navigate to="/login" replace />)}/> 
          <Route path="/login" element={<LoginPage />} /> 
          <Route path="/register" element={<RegisterPage />} /> 
          
            {/* Protected Routes */}
          <Route element={<ProtectedRoute />}>
            <Route path="/dashboard" element={<DashboardPage />}/>
            <Route path="/documents" element={<DocumentListPage />}/>
            <Route path="/documents/:id" element={<DocumentDetailPage />}/>
            <Route path="/flashcards" element={<FlashcardsListPage />}/>
            <Route path="/documents/:id/flashcards" element={<FlashcardPage />}/>
            <Route path="/quizzes/:quizId" element={<QuizTakePage />}/>
            <Route path="/quizzes/:quizId/results" element={<QuizResultPage />}/>
            <Route path="/profile" element={<ProfilePage />}/>
          </Route>


          <Route path="*" element={<NotFoundPage />}/>
        </Routes>
      </Router>
  
  ); 
}

export default App
