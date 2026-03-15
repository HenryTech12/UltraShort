import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Link2, Copy, CheckCircle2, AlertCircle, Zap, Shield, TrendingUp, Loader2 } from 'lucide-react';
import axios from 'axios';

interface ShortenResponse {
  shortUrl: string;
  urlId: string;
}

interface ToastMessage {
  id: number;
  type: 'success' | 'error';
  message: string;
}

function App() {
  const [url, setUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<ShortenResponse | null>(null);
  const [copied, setCopied] = useState(false);
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  const addToast = (type: 'success' | 'error', message: string) => {
    const id = Date.now();
    setToasts((prev) => [...prev, { id, type, message }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((toast) => toast.id !== id));
    }, 4000);
  };

  const handleShorten = async () => {
    if (!url.trim()) {
      addToast('error', 'Please enter a valid URL');
      return;
    }

    if (!url.startsWith('http://') && !url.startsWith('https://')) {
      addToast('error', 'URL must start with http:// or https://');
      return;
    }

    setLoading(true);
    setResult(null);
    setCopied(false);

    try {
      const response = await axios.post<ShortenResponse>(
        'https://ultrashort.onrender.com/short',
        { url },
        {
          headers: {
            'Content-Type': 'application/json',
          },
        }
      );

      setResult(response.data);
      addToast('success', 'URL shortened successfully!');
    } catch (error: any) {
      if (error.response?.status === 429) {
        addToast('error', 'Rate limit exceeded. Please try again later.');
      } else if (error.response?.status === 500) {
        addToast('error', 'Server error. Our team has been notified.');
      } else {
        addToast('error', 'Failed to shorten URL. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleCopy = async () => {
    if (result?.shortUrl) {
      await navigator.clipboard.writeText(result.shortUrl);
      setCopied(true);
      addToast('success', 'Copied to clipboard!');
      setTimeout(() => setCopied(false), 2000);
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleShorten();
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-950 via-gray-900 to-gray-950 text-white">
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_30%_20%,rgba(16,185,129,0.05),transparent_50%)]" />
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_70%_80%,rgba(16,185,129,0.03),transparent_50%)]" />

      <div className="relative max-w-5xl mx-auto px-4 py-12 sm:px-6 lg:px-8">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          className="text-center mb-16"
        >
          <div className="flex items-center justify-center mb-6">
            <motion.div
              initial={{ scale: 0 }}
              animate={{ scale: 1 }}
              transition={{ delay: 0.2, type: 'spring', stiffness: 200 }}
              className="bg-gradient-to-br from-emerald-500 to-emerald-600 p-4 rounded-2xl shadow-2xl shadow-emerald-500/20"
            >
              <Link2 className="w-10 h-10 text-white" />
            </motion.div>
          </div>

          <h1 className="text-5xl sm:text-6xl font-bold mb-4 bg-gradient-to-r from-white via-gray-100 to-gray-300 bg-clip-text text-transparent">
            UltraShort
          </h1>
          <p className="text-gray-400 text-lg max-w-2xl mx-auto leading-relaxed">
            Enterprise-grade URL shortening with circuit breaker protection and Redis-backed performance
          </p>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3, duration: 0.6 }}
          className="bg-gray-900/50 backdrop-blur-xl border border-gray-800 rounded-3xl p-8 sm:p-12 shadow-2xl mb-12"
        >
          <div className="space-y-6">
            <div className="relative">
              <input
                type="text"
                value={url}
                onChange={(e) => setUrl(e.target.value)}
                onKeyPress={handleKeyPress}
                placeholder="https://your-long-url-here.com"
                disabled={loading}
                className="w-full px-6 py-5 bg-gray-950/50 border border-gray-700 rounded-2xl text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed text-lg"
              />
            </div>

            <motion.button
              whileHover={{ scale: loading ? 1 : 1.02 }}
              whileTap={{ scale: loading ? 1 : 0.98 }}
              onClick={handleShorten}
              disabled={loading}
              className="w-full py-5 bg-gradient-to-r from-emerald-500 to-emerald-600 hover:from-emerald-600 hover:to-emerald-700 text-white font-semibold rounded-2xl shadow-lg shadow-emerald-500/30 transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed text-lg flex items-center justify-center gap-3"
            >
              {loading ? (
                <>
                  <Loader2 className="w-6 h-6 animate-spin" />
                  <span>Processing...</span>
                </>
              ) : (
                <>
                  <Zap className="w-6 h-6" />
                  <span>Shorten URL</span>
                </>
              )}
            </motion.button>
          </div>

          <AnimatePresence mode="wait">
            {loading && (
              <motion.div
                initial={{ opacity: 0, height: 0 }}
                animate={{ opacity: 1, height: 'auto' }}
                exit={{ opacity: 0, height: 0 }}
                className="mt-8 text-center"
              >
                <p className="text-gray-400 text-sm">
                  Starting service... This may take a moment on first request.
                </p>
              </motion.div>
            )}

            {result && !loading && (
              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                className="mt-8 bg-gray-950/50 border border-emerald-500/30 rounded-2xl p-6"
              >
                <div className="flex items-center justify-between mb-4">
                  <span className="text-sm text-gray-400 font-medium">Your Short URL</span>
                  <CheckCircle2 className="w-5 h-5 text-emerald-500" />
                </div>

                <div className="flex items-center gap-3 mb-4">
                  <input
                    type="text"
                    value={result.shortUrl}
                    readOnly
                    className="flex-1 px-4 py-3 bg-gray-900 border border-gray-700 rounded-xl text-emerald-400 font-mono text-sm sm:text-base focus:outline-none"
                  />
                  <motion.button
                    whileHover={{ scale: 1.05 }}
                    whileTap={{ scale: 0.95 }}
                    onClick={handleCopy}
                    className="px-6 py-3 bg-emerald-500 hover:bg-emerald-600 text-white rounded-xl transition-colors duration-200 flex items-center gap-2 font-medium"
                  >
                    {copied ? (
                      <>
                        <CheckCircle2 className="w-5 h-5" />
                        <span className="hidden sm:inline">Copied!</span>
                      </>
                    ) : (
                      <>
                        <Copy className="w-5 h-5" />
                        <span className="hidden sm:inline">Copy</span>
                      </>
                    )}
                  </motion.button>
                </div>

                <p className="text-xs text-gray-500 leading-relaxed">
                  Short links redirect via HTTP 302 status code for instant delivery.
                </p>
              </motion.div>
            )}
          </AnimatePresence>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5, duration: 0.6 }}
          className="bg-gray-900/30 backdrop-blur-xl border border-gray-800 rounded-3xl p-8 sm:p-10"
        >
          <div className="flex items-center justify-between mb-8">
            <h2 className="text-2xl font-bold text-white">System Status</h2>
            <div className="flex items-center gap-2">
              <div className="w-2 h-2 bg-emerald-500 rounded-full animate-pulse" />
              <span className="text-emerald-500 font-semibold text-sm">Active</span>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
            <div className="bg-gray-950/50 border border-gray-800 rounded-2xl p-6 hover:border-emerald-500/30 transition-colors duration-300">
              <Shield className="w-8 h-8 text-emerald-500 mb-4" />
              <h3 className="font-semibold text-white mb-2">Circuit Breaker</h3>
              <p className="text-sm text-gray-400 leading-relaxed">
                Automatic failure detection and recovery for maximum uptime
              </p>
            </div>

            <div className="bg-gray-950/50 border border-gray-800 rounded-2xl p-6 hover:border-emerald-500/30 transition-colors duration-300">
              <Zap className="w-8 h-8 text-emerald-500 mb-4" />
              <h3 className="font-semibold text-white mb-2">Redis Cache</h3>
              <p className="text-sm text-gray-400 leading-relaxed">
                Lightning-fast URL retrieval with distributed caching
              </p>
            </div>

            <div className="bg-gray-950/50 border border-gray-800 rounded-2xl p-6 hover:border-emerald-500/30 transition-colors duration-300">
              <TrendingUp className="w-8 h-8 text-emerald-500 mb-4" />
              <h3 className="font-semibold text-white mb-2">Rate Limited</h3>
              <p className="text-sm text-gray-400 leading-relaxed">
                Smart throttling to ensure fair usage and service stability
              </p>
            </div>
          </div>
        </motion.div>
      </div>

      <div className="fixed top-4 right-4 z-50 space-y-3">
        <AnimatePresence>
          {toasts.map((toast) => (
            <motion.div
              key={toast.id}
              initial={{ opacity: 0, x: 100, scale: 0.8 }}
              animate={{ opacity: 1, x: 0, scale: 1 }}
              exit={{ opacity: 0, x: 100, scale: 0.8 }}
              className={`flex items-center gap-3 px-6 py-4 rounded-2xl shadow-2xl backdrop-blur-xl border ${
                toast.type === 'success'
                  ? 'bg-emerald-500/20 border-emerald-500/50 text-emerald-100'
                  : 'bg-red-500/20 border-red-500/50 text-red-100'
              }`}
            >
              {toast.type === 'success' ? (
                <CheckCircle2 className="w-5 h-5 flex-shrink-0" />
              ) : (
                <AlertCircle className="w-5 h-5 flex-shrink-0" />
              )}
              <span className="text-sm font-medium">{toast.message}</span>
            </motion.div>
          ))}
        </AnimatePresence>
      </div>
    </div>
  );
}

export default App;
