import './Navbar.css'
import { useNavigate } from "react-router";

const Navbar = () => {

  const isLoggedIn = false;  
  const navigate = useNavigate();

  return (
    <>
      <div className="nav-body">
        <div className="nav-logo"><img src="../src/assets/nav-logo.png" /></div>
        <div className="nav-links">
          <a href="/">Chat</a>
          <a href="/quiz">Quiz</a>
        </div> 
        {isLoggedIn ? (
        <div className="nav-profile">
          <img
            src=""
            alt="Profile"
            className="profile-img"
          />
        </div>
      ) : (
        <div className="nav-login">
          <button onClick={() => navigate("/login")}>Login</button>
        </div>
      )}      </div>
    </>
  )
}

export default Navbar
