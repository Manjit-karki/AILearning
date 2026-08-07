import React from 'react';
import { useNavigate } from 'react-router-dom';
import { FileText, Trash2, BookOpen, BrainCircuit, Clock } from 'lucide-react';
import moment from 'moment';

// Helper function to format file size
const formatFileSize = (bytes) => {
  if (bytes === undefined || bytes === null) return 'N/A';

  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let size = bytes;
  let unitIndex = 0;

  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024;
    unitIndex++;
  }

  return `${size.toFixed(1)} ${units[unitIndex]}`;
};

const DocumentCard = ({ document, onDelete }) => {
  const navigate = useNavigate();

  const handleNavigate = () => {
    navigate(`/documents/${document._id}`);
  };

  const handleDelete = (e) => {
    e.stopPropagation();
    onDelete(document);
  };

  return (
    <div
      className="group relative flex flex-col justify-between p-5 bg-slate-50/80 hover:bg-white rounded-2xl border border-slate-100 shadow-sm hover:shadow-md transition-all duration-200 cursor-pointer overflow-hidden max-w-xs"
      onClick={handleNavigate}
    >
      {/* Header Section */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <div className="p-3 bg-emerald-500 text-white rounded-xl shadow-sm">
            <FileText className="w-6 h-6" strokeWidth={2} />
          </div>
          <button
            onClick={handleDelete}
            className="p-1.5 text-slate-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors"
          >
            <Trash2 className="w-4 h-4" strokeWidth={2} />
          </button>
        </div>

        {/* Title */}
        <h3 className="font-semibold text-slate-800 text-base mb-1 line-clamp-1" title={document.title}>
          {document.title}
        </h3>

        {/* Document Info */}
        <div className="text-xs text-slate-400 font-medium mb-4">
          {document.fileSize !== undefined && (
            <>
              <span>{formatFileSize(document.fileSize)}</span>
            </>
          )}
        </div>

        {/* Stats Section */}
        <div className="flex items-center gap-2 mb-4">
          {document.flashcardCount !== undefined && (
            <div className="flex items-center gap-1.5 px-2.5 py-1 bg-purple-50 text-purple-600 rounded-lg text-xs font-medium">
              <BookOpen className="w-3.5 h-3.5" strokeWidth={2} />
              <span>{document.flashcardCount} Flashcards</span>
            </div>
          )}
          {document.quizCount !== undefined && (
            <div className="flex items-center gap-1.5 px-2.5 py-1 bg-emerald-50 text-emerald-600 rounded-lg text-xs font-medium">
              <BrainCircuit className="w-3.5 h-3.5" strokeWidth={2} />
              <span>{document.quizCount} Quizzes</span>
            </div>
          )}
        </div>
      </div>

      {/* Footer Section */}
      <div className="pt-3 border-t border-slate-100/80">
        <div className="flex items-center gap-1.5 text-xs text-slate-400">
          <Clock className="w-3.5 h-3.5" strokeWidth={2} />
          <span>Uploaded {moment(document.createdAt).fromNow()}</span>
        </div>
      </div>

      {/* Hover Indicator */}
      <div className="absolute bottom-0 left-0 right-0 h-1 bg-emerald-500 opacity-0 group-hover:opacity-100 transition-opacity duration-200" />
    </div>
  );
};

export default DocumentCard;