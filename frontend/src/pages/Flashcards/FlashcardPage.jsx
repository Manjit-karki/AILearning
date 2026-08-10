import { useState, useEffect } from 'react';
import { useParams, Link } from "react-router-dom";
import { ArrowLeft, Plus, ChevronLeft, ChevronRight, Trash2, } from 'lucide-react';
import toast from 'react-hot-toast';


// import flashcardService from "../../services/flashcardService";
// import aiService from "../../services/aiSerivce";
// import PageHeader from "../../Components/common/Pageheader";
// import Spinner from "../../Components/common/Spinner";
// import EmptyState from "../../Components/common/EmptyState";
// import Button from "../../Components/common/Button";
// import Modal from "../../Components/common/Modal";
// import Flashcard from "../../Component/flashcards/Flashcard";
//

const FlashcardPage = () => {

  const { id: documentId } = useParams();
  const [flashcardSets, setFlashcardSets] = useState([]);
  const [flashcards, setFlashcards] = useState([]);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [currentCardIndex, setCurrentCardIndex] = useState(0);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const fetchFlashcards = async () => {
    setLoading(true);
    try {
      const response = await flashcardService.getFlashcardsForDocument(
        documentId
      );
      setFlashcardSets(response.data[0]);
      setFlashcards(response.data[0]?.cards || []);
    } catch (error) {
      toast.error("Failed to fetch flashcards.");
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchFlashcards();
  }, [documentId]);





  return (
    <div></div>
  )
}

export default FlashcardPage
