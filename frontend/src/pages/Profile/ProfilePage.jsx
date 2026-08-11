import React, { useState, useEffect } from "react";
import PageHeader from "../../components/common/PageHeader";
import Button from "../../components/common/Button";
import Spinner from "../../components/common/Spinner";
import authService from "../../services/authService";
import { useAuth } from "../../context/AuthContext";
import toast from "react-hot-toast";
import { User, Mail, Lock } from "lucide-react";

const ProfilePage = () => {
  const [loading, setLoading] = useState(true);
  const [passwordLoading, setPasswordLoading] = useState(false);

  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmNewPassword, setConfirmNewPassword] = useState("");

  return (
    <div>
      <PageHeader title="Profile Settings" />

      <div className="">
        {/* User Information Display */}
        <div className="">
          <h3 className="">
            User Information
          </h3>
          
          <div className="">
            {/* Username Section */}
            <div>
              <label className="">
                Username
              </label>
              <div className="">
                <div className="">
                  <User className="" />
                </div>
                <p className="">
                  {username}
                </p>
              </div>
            </div>
            
            {/* Email Section */}
            <div>
              <label className="">
                Email Address
              </label>
              <div className="">
                <div className="">
                  <Mail className="" />
                </div>
                <p className="">
                  {email}
                </p>
              </div>
            </div>

          </div>
        </div>
      </div>
    </div>
  );
};

export default ProfilePage;