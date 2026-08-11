import { useState, useEffect } from "react";
import { useParams, Link } from "react-router-dom";
import {
  ArrowLeft,
  Plus,
  ChevronLeft,
  ChevronRight,
  Trash2,
} from "lucide-react";
import toast from "react-hot-toast";

import flashcardService from "../../services/flashcardService";
import aiService from "../../services/aiService";
import PageHeader from "../../Components/common/PageHeader";
import Spinner from "../../Components/common/Spinner";
import EmptyState from "../../Components/common/EmptyState";
import Button from "../../Components/common/Button";
import Modal from "../../Components/common/Modal";
import Flashcard from "../../Components/flashcards/Flashcard";

const FlashcardPage = () => {
  const { id: documentId } = useParams();

  const [flashcardSets, setFlashcardSets] = useState(null);
  const [flashcards, setFlashcards] = useState([]);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [currentCardIndex, setCurrentCardIndex] = useState(0);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const fetchFlashcards = async () => {
    setLoading(true);

    try {
      const response =
        await flashcardService.getFlashcardsForDocument(documentId);

      const sets = response.data || [];
      const firstSet = sets[0] || null;

      setFlashcardSets(firstSet);
      setFlashcards(firstSet?.cards || []);
      setCurrentCardIndex(0);
    } catch (error) {
      toast.error("Failed to fetch flashcards.");
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (documentId) {
      fetchFlashcards();
    }
  }, [documentId]);

  const handleGenerateFlashcards = async () => {
    setGenerating(true);

    try {
      await aiService.generateFlashcards(documentId);
      toast.success("Flashcards generated successfully!");
      await fetchFlashcards();
    } catch (error) {
      toast.error(
        error.message || "Failed to generate flashcards."
      );
    } finally {
      setGenerating(false);
    }
  };

  const handlePrevCard = () => {
    if (flashcards.length <= 1) return;

    handleReview(currentCardIndex);

    setCurrentCardIndex(
      (prevIndex) =>
        (prevIndex - 1 + flashcards.length) % flashcards.length
    );
  };

  const handleNextCard = () => {
    if (flashcards.length <= 1) return;

    handleReview(currentCardIndex);

    setCurrentCardIndex(
      (prevIndex) => (prevIndex + 1) % flashcards.length
    );
  };

  const handleReview = async (index) => {
    const currentCard = flashcards[index];

    if (!currentCard) return;

    try {
      await flashcardService.reviewFlashcard(currentCard._id, index);
    } catch (error) {
      toast.error("Failed to review flashcard.");
      console.error(error);
    }
  };

  const handleToggleStar = async (cardId) => {
    try {
      await flashcardService.toggleStar(cardId);

      setFlashcards((prevFlashcards) =>
        prevFlashcards.map((card) =>
          card._id === cardId
            ? { ...card, isStarred: !card.isStarred }
            : card
        )
      );

      toast.success("Flashcard star status updated!");
    } catch (error) {
      toast.error("Failed to update star status.");
      console.error(error);
    }
  };

  const handleDeleteFlashcardSet = async () => {
    if (!flashcardSets?._id) return;

    setDeleting(true);

    try {
      await flashcardService.deleteFlashcardSet(flashcardSets._id);

      toast.success("Flashcard set deleted successfully!");

      setIsDeleteModalOpen(false);
      setFlashcardSets(null);
      setFlashcards([]);
      setCurrentCardIndex(0);
    } catch (error) {
      toast.error(
        error.message || "Failed to delete flashcard set."
      );
      console.error(error);
    } finally {
      setDeleting(false);
    }
  };

  const renderFlashcardContent = () => {
    if (loading) {
      return (
        <div className="flex justify-center py-12">
          <Spinner />
        </div>
      );
    }

    if (flashcards.length === 0) {
      return (
        <EmptyState
          title="No Flashcards Yet"
          description="Generate flashcards from your document."
          buttonText="Generate Flashcards"
          onActionClick={handleGenerateFlashcards}
        />
      );
    }

    const currentCard = flashcards[currentCardIndex];

    return (
      <div className="flex flex-col items-center space-y-6">
        <div className="w-full max-w-md">
          <Flashcard
            flashcard={currentCard}
            onToggleStar={handleToggleStar}
          />
        </div>

        <div className="flex items-center gap-4">
          <Button
            onClick={handlePrevCard}
            variant="secondary"
            disabled={flashcards.length <= 1}
          >
            <ChevronLeft size={16} />
            Previous
          </Button>

          <span className="text-sm text-neutral-600 font-medium">
            {currentCardIndex + 1} / {flashcards.length}
          </span>

          <Button
            onClick={handleNextCard}
            variant="secondary"
            disabled={flashcards.length <= 1}
          >
            Next
            <ChevronRight size={16} />
          </Button>
        </div>
      </div>
    );
  };

  return (
    <div>
      <div className="mb-4">
        <Link
          to={`/documents/${documentId}`}
          className="inline-flex items-center gap-2 text-sm text-neutral-600 hover:text-neutral-900 transition-colors"
        >
          <ArrowLeft size={16} />
          Back to Document
        </Link>
      </div>

      <PageHeader title="Flashcards">
        <div className="flex gap-2">
          {!loading && flashcards.length > 0 && (
            <Button
              onClick={() => setIsDeleteModalOpen(true)}
              disabled={deleting}
              variant="secondary"
            >
              <Trash2 size={16} />
              Delete Set
            </Button>
          )}

          {!loading && flashcards.length === 0 && (
            <Button
              onClick={handleGenerateFlashcards}
              disabled={generating}
            >
              {generating ? (
                <>
                  <Spinner />
                  Generating...
                </>
              ) : (
                <>
                  <Plus size={16} />
                  Generate Flashcards
                </>
              )}
            </Button>
          )}
        </div>
      </PageHeader>

      {renderFlashcardContent()}

      <Modal
        isOpen={isDeleteModalOpen}
        onClose={() => !deleting && setIsDeleteModalOpen(false)}
        title="Confirm Delete Flashcard Set"
      >
        <div className="space-y-4">
          <p className="text-sm text-neutral-600">
            Are you sure you want to delete all flashcards for this
            document? This action cannot be undone.
          </p>

          <div className="flex justify-end gap-2 pt-2">
            <Button
              type="button"
              variant="secondary"
              onClick={() => setIsDeleteModalOpen(false)}
              disabled={deleting}
            >
              Cancel
            </Button>

            <Button
              type="button"
              onClick={handleDeleteFlashcardSet}
              disabled={deleting}
              className="bg-red-500 hover:bg-red-600 text-white"
            >
              {deleting ? "Deleting..." : "Delete"}
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};

export default FlashcardPage;
