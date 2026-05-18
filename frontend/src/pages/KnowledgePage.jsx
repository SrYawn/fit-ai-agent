import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'

const CATEGORIES = [
  { value: 'exercise', label: '健身动作' },
  { value: 'injury-recovery', label: '损伤恢复' },
  { value: 'nutrition', label: '营养饮食' },
  { value: 'training-plan', label: '训练计划' },
  { value: 'body-knowledge', label: '运动生理' },
  { value: 'motivation', label: '运动心理' },
]

export default function KnowledgePage({ user }) {
  const [documents, setDocuments] = useState([])
  const [loading, setLoading] = useState(false)
  const [initLoading, setInitLoading] = useState(false)
  const [message, setMessage] = useState(null)
  const [filterCategory, setFilterCategory] = useState('')
  const [showUploadModal, setShowUploadModal] = useState(false)
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)
  const [total, setTotal] = useState(0)

  useEffect(() => {
    setPage(0)
  }, [filterCategory, pageSize])

  useEffect(() => {
    loadDocuments()
  }, [filterCategory, page, pageSize])

  const loadDocuments = async () => {
    setLoading(true)
    try {
      const params = new URLSearchParams()
      if (filterCategory) params.set('category', filterCategory)
      params.set('page', page.toString())
      params.set('size', pageSize.toString())
      const res = await fetch(`/api/fitness/knowledge/list?${params}`)
      const data = await res.json()
      if (data.success) {
        setDocuments(data.documents || [])
        setTotal(data.total || 0)
      } else {
        setMessage({ type: 'error', text: data.message || '加载失败' })
      }
    } catch (err) {
      setMessage({ type: 'error', text: '网络错误' })
    } finally {
      setLoading(false)
    }
  }

  const handleInit = async () => {
    setInitLoading(true)
    setMessage(null)
    try {
      const res = await fetch('/api/fitness/knowledge/init', { method: 'POST' })
      const data = await res.json()
      if (data.success) {
        setMessage({ type: 'success', text: `初始化完成，导入 ${data.documentCount} 个文档片段` })
        loadDocuments()
      } else {
        setMessage({ type: 'error', text: data.message || '初始化失败' })
      }
    } catch (err) {
      setMessage({ type: 'error', text: '初始化请求失败' })
    } finally {
      setInitLoading(false)
    }
  }

  const handleUpload = async (file, category) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('category', category)
    setMessage(null)
    try {
      const res = await fetch('/api/fitness/knowledge/upload', { method: 'POST', body: formData })
      const data = await res.json()
      if (data.success) {
        setMessage({ type: 'success', text: `上传成功: ${data.filename}，${data.documentCount} 个片段` })
        loadDocuments()
      } else {
        setMessage({ type: 'error', text: data.message || '上传失败' })
      }
    } catch (err) {
      setMessage({ type: 'error', text: '上传请求失败' })
    }
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white border-b border-gray-200 px-4 py-3 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <h1 className="text-lg font-semibold text-gray-800">知识库管理</h1>
          <span className="text-sm text-gray-500">· {user.username}</span>
        </div>
        <Link to="/chat" className="text-sm text-indigo-500 hover:text-indigo-700">返回问答</Link>
      </header>

      <div className="max-w-4xl mx-auto p-6 space-y-6">
        {message && (
          <div className={`p-3 rounded-lg text-sm ${
            message.type === 'success' ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-600'
          }`}>{message.text}</div>
        )}

        {/* 操作区 */}
        <div className="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
          <div className="flex items-center gap-3">
            <button
              onClick={handleInit}
              disabled={initLoading}
              className="px-4 py-2 bg-indigo-500 text-white rounded-lg hover:bg-indigo-600
                         disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors text-sm"
            >
              {initLoading ? '初始化中...' : '初始化知识库'}
            </button>
            <button
              onClick={() => setShowUploadModal(true)}
              className="px-4 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600 transition-colors text-sm"
            >
              新增文档
            </button>
          </div>
        </div>

        {/* 上传模态框 */}
        {showUploadModal && (
          <UploadModal
            categories={CATEGORIES}
            onUpload={handleUpload}
            onClose={() => setShowUploadModal(false)}
          />
        )}

        {/* 知识库条目列表 */}
        <div className="bg-white rounded-xl p-5 shadow-sm border border-gray-100 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="font-medium text-gray-800">知识库条目 ({total})</h2>
            <div className="flex items-center gap-3">
              <select
                value={pageSize}
                onChange={(e) => setPageSize(Number(e.target.value))}
                className="border border-gray-300 rounded-lg px-3 py-2 text-sm"
              >
                <option value={10}>10 条/页</option>
                <option value={20}>20 条/页</option>
                <option value={50}>50 条/页</option>
              </select>
              <select
                value={filterCategory}
                onChange={(e) => setFilterCategory(e.target.value)}
                className="border border-gray-300 rounded-lg px-3 py-2 text-sm"
              >
                <option value="">全部分类</option>
                {CATEGORIES.map(c => <option key={c.value} value={c.value}>{c.label}</option>)}
              </select>
            </div>
          </div>

          {loading ? (
            <div className="text-center text-gray-400 py-8">加载中...</div>
          ) : documents.length === 0 ? (
            <div className="text-center text-gray-400 py-8">
              暂无知识库条目，请先点击「初始化知识库」
            </div>
          ) : (
            <div className="space-y-3">
              {documents.map((doc, i) => (
                <div key={doc.id || i} className="border border-gray-100 rounded-lg p-3 text-sm">
                  <div className="flex items-center gap-2 mb-1">
                    <span className="px-2 py-0.5 bg-indigo-50 text-indigo-600 rounded text-xs">
                      {doc.metadata?.category || '未分类'}
                    </span>
                    {doc.metadata?.filename && (
                      <span className="text-gray-400 text-xs">{doc.metadata.filename}</span>
                    )}
                  </div>
                  <p className="text-gray-600 leading-relaxed">{doc.content}</p>
                </div>
              ))}
            </div>
          )}

          {/* 分页控件 */}
          {total > 0 && (
            <div className="flex items-center justify-between pt-3 border-t border-gray-100">
              <span className="text-sm text-gray-500">
                第 {page * pageSize + 1}-{Math.min((page + 1) * pageSize, total)} 条，共 {total} 条
              </span>
              <div className="flex items-center gap-2">
                <button
                  onClick={() => setPage(p => p - 1)}
                  disabled={page === 0}
                  className="px-3 py-1.5 text-sm border border-gray-300 rounded-lg
                             hover:bg-gray-50 disabled:text-gray-300 disabled:cursor-not-allowed"
                >
                  上一页
                </button>
                <span className="text-sm text-gray-600">
                  {page + 1} / {Math.ceil(total / pageSize)}
                </span>
                <button
                  onClick={() => setPage(p => p + 1)}
                  disabled={(page + 1) * pageSize >= total}
                  className="px-3 py-1.5 text-sm border border-gray-300 rounded-lg
                             hover:bg-gray-50 disabled:text-gray-300 disabled:cursor-not-allowed"
                >
                  下一页
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

function UploadModal({ categories, onUpload, onClose }) {
  const [category, setCategory] = useState(categories[0].value)
  const [file, setFile] = useState(null)
  const [uploading, setUploading] = useState(false)

  const handleSubmit = async () => {
    if (!file) return
    setUploading(true)
    await onUpload(file, category)
    setUploading(false)
    onClose()
  }

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
      <div className="bg-white rounded-2xl p-6 w-full max-w-md shadow-xl space-y-4">
        <h3 className="text-lg font-semibold text-gray-800">新增知识文档</h3>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">分类</label>
          <select value={category} onChange={(e) => setCategory(e.target.value)}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-indigo-400">
            {categories.map(c => <option key={c.value} value={c.value}>{c.label}</option>)}
          </select>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">选择文件 (.md)</label>
          <input type="file" accept=".md" onChange={(e) => setFile(e.target.files[0])}
            className="w-full text-sm text-gray-600 file:mr-3 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:bg-indigo-50 file:text-indigo-600 hover:file:bg-indigo-100" />
        </div>
        <div className="flex justify-end gap-3 pt-2">
          <button onClick={onClose} className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800">取消</button>
          <button onClick={handleSubmit} disabled={!file || uploading}
            className="px-4 py-2 bg-indigo-500 text-white rounded-lg text-sm hover:bg-indigo-600 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors">
            {uploading ? '上传中...' : '确认上传'}
          </button>
        </div>
      </div>
    </div>
  )
}