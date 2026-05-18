import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'

const GENDER_OPTIONS = [
  { value: '', label: '请选择' },
  { value: 'MALE', label: '男' },
  { value: 'FEMALE', label: '女' },
  { value: 'OTHER', label: '其他' },
]

const LEVEL_OPTIONS = [
  { value: 'BEGINNER', label: '初学者' },
  { value: 'INTERMEDIATE', label: '中级' },
  { value: 'ADVANCED', label: '高级' },
]

export default function RegisterPage() {
  const [form, setForm] = useState({
    username: '', password: '', confirmPassword: '',
    age: '', gender: '', height: '', weight: '',
    fitnessGoal: '', fitnessLevel: 'BEGINNER'
  })
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const updateField = (field, value) => setForm(prev => ({ ...prev, [field]: value }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!form.username.trim() || !form.password) {
      setError('用户名和密码不能为空'); return
    }
    if (form.password !== form.confirmPassword) {
      setError('两次输入的密码不一致'); return
    }
    setLoading(true); setError(null)
    try {
      const res = await fetch('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(form)
      })
      const data = await res.json()
      if (data.success) {
        navigate('/', { state: { registered: true } })
      } else {
        setError(data.message || '注册失败')
      }
    } catch (err) {
      setError('网络错误，请确认后端服务已启动')
    } finally { setLoading(false) }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-6">
          <h1 className="text-3xl font-bold text-gray-800">FitAI</h1>
          <p className="text-gray-500 mt-2">创建新账号</p>
        </div>

        <form onSubmit={handleSubmit} className="bg-white rounded-2xl shadow-sm p-6 space-y-4">
          {error && (
            <div className="bg-red-50 text-red-600 text-sm p-3 rounded-lg">{error}</div>
          )}

          <div className="grid grid-cols-1 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">用户名 *</label>
              <input type="text" value={form.username} onChange={(e) => updateField('username', e.target.value)}
                placeholder="请输入用户名"
                className="w-full px-4 py-2 border border-gray-300 rounded-xl focus:outline-none focus:border-indigo-400 focus:ring-1 focus:ring-indigo-400" />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">密码 *</label>
              <input type="password" value={form.password} onChange={(e) => updateField('password', e.target.value)}
                placeholder="请输入密码"
                className="w-full px-4 py-2 border border-gray-300 rounded-xl focus:outline-none focus:border-indigo-400 focus:ring-1 focus:ring-indigo-400" />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">确认密码 *</label>
              <input type="password" value={form.confirmPassword} onChange={(e) => updateField('confirmPassword', e.target.value)}
                placeholder="再次输入密码"
                className="w-full px-4 py-2 border border-gray-300 rounded-xl focus:outline-none focus:border-indigo-400 focus:ring-1 focus:ring-indigo-400" />
            </div>
          </div>

          <hr className="border-gray-200" />
          <p className="text-xs text-gray-500">以下为选填信息，帮助 AI 更好地为你服务</p>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">年龄</label>
              <input type="number" value={form.age} onChange={(e) => updateField('age', e.target.value)}
                placeholder="如 25"
                className="w-full px-3 py-2 border border-gray-300 rounded-xl focus:outline-none focus:border-indigo-400 focus:ring-1 focus:ring-indigo-400" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">性别</label>
              <select value={form.gender} onChange={(e) => updateField('gender', e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-xl focus:outline-none focus:border-indigo-400 focus:ring-1 focus:ring-indigo-400">
                {GENDER_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">身高(cm)</label>
              <input type="number" step="0.01" value={form.height} onChange={(e) => updateField('height', e.target.value)}
                placeholder="如 175"
                className="w-full px-3 py-2 border border-gray-300 rounded-xl focus:outline-none focus:border-indigo-400 focus:ring-1 focus:ring-indigo-400" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">体重(kg)</label>
              <input type="number" step="0.01" value={form.weight} onChange={(e) => updateField('weight', e.target.value)}
                placeholder="如 70"
                className="w-full px-3 py-2 border border-gray-300 rounded-xl focus:outline-none focus:border-indigo-400 focus:ring-1 focus:ring-indigo-400" />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">健身目标</label>
            <input type="text" value={form.fitnessGoal} onChange={(e) => updateField('fitnessGoal', e.target.value)}
              placeholder="如：增肌、减脂、提高体能"
              className="w-full px-3 py-2 border border-gray-300 rounded-xl focus:outline-none focus:border-indigo-400 focus:ring-1 focus:ring-indigo-400" />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">健身水平</label>
            <select value={form.fitnessLevel} onChange={(e) => updateField('fitnessLevel', e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-xl focus:outline-none focus:border-indigo-400 focus:ring-1 focus:ring-indigo-400">
              {LEVEL_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
            </select>
          </div>

          <button type="submit" disabled={loading || !form.username.trim() || !form.password}
            className="w-full py-2.5 bg-indigo-500 text-white rounded-xl font-medium hover:bg-indigo-600 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors">
            {loading ? '注册中...' : '注册'}
          </button>

          <p className="text-sm text-center text-gray-500">
            已有账号？<Link to="/" className="text-indigo-500 hover:text-indigo-700">返回登录</Link>
          </p>
        </form>
      </div>
    </div>
  )
}
