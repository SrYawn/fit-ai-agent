import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'

const SEVERITY_OPTIONS = [
  { value: 'MILD', label: '轻微' },
  { value: 'MODERATE', label: '中等' },
  { value: 'SEVERE', label: '严重' },
]

const RECOVERY_OPTIONS = [
  { value: 'RECOVERING', label: '恢复中' },
  { value: 'RECOVERED', label: '已恢复' },
  { value: 'CHRONIC', label: '慢性' },
]

const COMPLETION_OPTIONS = [
  { value: 'COMPLETED', label: '已完成' },
  { value: 'PARTIAL', label: '部分完成' },
  { value: 'SKIPPED', label: '跳过' },
]

export default function UserRecordsPage({ user }) {
  const [tab, setTab] = useState('injuries')
  const [injuries, setInjuries] = useState([])
  const [records, setRecords] = useState([])
  const [showForm, setShowForm] = useState(false)
  const [editingItem, setEditingItem] = useState(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => { loadData() }, [tab])

  const loadData = async () => {
    setLoading(true)
    try {
      const endpoint = tab === 'injuries' ? '/api/user/injuries' : '/api/user/training-records'
      const res = await fetch(`${endpoint}?userId=${user.id}`)
      const data = await res.json()
      if (data.success) {
        if (tab === 'injuries') setInjuries(data.data)
        else setRecords(data.data)
      }
    } catch (err) { console.error(err) }
    finally { setLoading(false) }
  }

  const handleDelete = async (id) => {
    if (!confirm('确定删除？')) return
    const endpoint = tab === 'injuries' ? `/api/user/injuries/${id}` : `/api/user/training-records/${id}`
    await fetch(endpoint, { method: 'DELETE' })
    loadData()
  }

  const handleEdit = (item) => {
    setEditingItem(item)
    setShowForm(true)
  }

  const handleAdd = () => {
    setEditingItem(null)
    setShowForm(true)
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white border-b border-gray-200 px-4 py-3 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <h1 className="text-lg font-semibold text-gray-800">我的记录</h1>
          <span className="text-sm text-gray-500">· {user.username}</span>
        </div>
        <Link to="/chat" className="text-sm text-indigo-500 hover:text-indigo-700">返回问答</Link>
      </header>

      <div className="max-w-4xl mx-auto p-6 space-y-4">
        <div className="flex gap-2">
          <button onClick={() => setTab('injuries')}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${tab === 'injuries' ? 'bg-indigo-500 text-white' : 'bg-white text-gray-600 border border-gray-300 hover:bg-gray-50'}`}>
            伤病记录
          </button>
          <button onClick={() => setTab('training')}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${tab === 'training' ? 'bg-indigo-500 text-white' : 'bg-white text-gray-600 border border-gray-300 hover:bg-gray-50'}`}>
            训练记录
          </button>
          <button onClick={handleAdd}
            className="ml-auto px-4 py-2 bg-green-500 text-white rounded-lg text-sm hover:bg-green-600 transition-colors">
            新增记录
          </button>
        </div>

        {showForm && (
          <RecordForm tab={tab} item={editingItem} userId={user.id}
            onClose={() => { setShowForm(false); setEditingItem(null) }}
            onSaved={() => { setShowForm(false); setEditingItem(null); loadData() }} />
        )}

        {loading ? (
          <div className="text-center text-gray-400 py-8">加载中...</div>
        ) : tab === 'injuries' ? (
          <InjuryList items={injuries} onEdit={handleEdit} onDelete={handleDelete} />
        ) : (
          <TrainingList items={records} onEdit={handleEdit} onDelete={handleDelete} />
        )}
      </div>
    </div>
  )
}

function InjuryList({ items, onEdit, onDelete }) {
  if (items.length === 0) return <div className="text-center text-gray-400 py-8">暂无伤病记录</div>
  return (
    <div className="space-y-3">
      {items.map(item => (
        <div key={item.id} className="bg-white rounded-xl p-4 shadow-sm border border-gray-100">
          <div className="flex items-center justify-between mb-2">
            <div className="flex items-center gap-2">
              <span className="font-medium text-gray-800">{item.injury_type}</span>
              <span className="text-sm text-gray-500">· {item.injury_location}</span>
              <span className={`px-2 py-0.5 rounded text-xs ${
                item.severity === 'SEVERE' ? 'bg-red-50 text-red-600' :
                item.severity === 'MODERATE' ? 'bg-yellow-50 text-yellow-600' : 'bg-green-50 text-green-600'
              }`}>{item.severity === 'SEVERE' ? '严重' : item.severity === 'MODERATE' ? '中等' : '轻微'}</span>
            </div>
            <div className="flex gap-2">
              <button onClick={() => onEdit(item)} className="text-xs text-indigo-500 hover:text-indigo-700">编辑</button>
              <button onClick={() => onDelete(item.id)} className="text-xs text-red-500 hover:text-red-700">删除</button>
            </div>
          </div>
          {item.description && <p className="text-sm text-gray-600 mb-1">{item.description}</p>}
          <div className="flex gap-3 text-xs text-gray-400">
            <span>状态: {item.recovery_status === 'RECOVERED' ? '已恢复' : item.recovery_status === 'CHRONIC' ? '慢性' : '恢复中'}</span>
            {item.injury_date && <span>受伤日期: {item.injury_date}</span>}
          </div>
        </div>
      ))}
    </div>
  )
}

function TrainingList({ items, onEdit, onDelete }) {
  if (items.length === 0) return <div className="text-center text-gray-400 py-8">暂无训练记录</div>
  return (
    <div className="space-y-3">
      {items.map(item => (
        <div key={item.id} className="bg-white rounded-xl p-4 shadow-sm border border-gray-100">
          <div className="flex items-center justify-between mb-2">
            <div className="flex items-center gap-2">
              <span className="font-medium text-gray-800">{item.exercise_name}</span>
              <span className="text-sm text-gray-500">· {item.training_type}</span>
              <span className={`px-2 py-0.5 rounded text-xs ${
                item.completion_status === 'COMPLETED' ? 'bg-green-50 text-green-600' :
                item.completion_status === 'PARTIAL' ? 'bg-yellow-50 text-yellow-600' : 'bg-gray-100 text-gray-500'
              }`}>{item.completion_status === 'COMPLETED' ? '已完成' : item.completion_status === 'PARTIAL' ? '部分完成' : '跳过'}</span>
            </div>
            <div className="flex gap-2">
              <button onClick={() => onEdit(item)} className="text-xs text-indigo-500 hover:text-indigo-700">编辑</button>
              <button onClick={() => onDelete(item.id)} className="text-xs text-red-500 hover:text-red-700">删除</button>
            </div>
          </div>
          <div className="flex flex-wrap gap-3 text-xs text-gray-500">
            <span>日期: {item.training_date}</span>
            {item.sets && <span>{item.sets}组 x {item.reps}次</span>}
            {item.weight && <span>{item.weight}kg</span>}
            {item.duration && <span>{item.duration}分钟</span>}
            {item.calories_burned && <span>{item.calories_burned}卡</span>}
          </div>
          {item.notes && <p className="text-sm text-gray-500 mt-1">{item.notes}</p>}
        </div>
      ))}
    </div>
  )
}

function RecordForm({ tab, item, userId, onClose, onSaved }) {
  const isEdit = !!item
  const [form, setForm] = useState(() => {
    if (tab === 'injuries') {
      return item ? {
        injuryType: item.injury_type || '', injuryLocation: item.injury_location || '',
        severity: item.severity || 'MILD', description: item.description || '',
        recoveryStatus: item.recovery_status || 'RECOVERING', injuryDate: item.injury_date || ''
      } : { injuryType: '', injuryLocation: '', severity: 'MILD', description: '', recoveryStatus: 'RECOVERING', injuryDate: '' }
    }
    return item ? {
      trainingDate: item.training_date || '', trainingType: item.training_type || '',
      exerciseName: item.exercise_name || '', sets: item.sets || '', reps: item.reps || '',
      weight: item.weight || '', duration: item.duration || '', caloriesBurned: item.calories_burned || '',
      completionStatus: item.completion_status || 'COMPLETED', notes: item.notes || ''
    } : { trainingDate: '', trainingType: '', exerciseName: '', sets: '', reps: '', weight: '', duration: '', caloriesBurned: '', completionStatus: 'COMPLETED', notes: '' }
  })
  const [saving, setSaving] = useState(false)

  const updateField = (field, value) => setForm(prev => ({ ...prev, [field]: value }))
  const inputCls = "w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:border-indigo-400"

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSaving(true)
    const endpoint = tab === 'injuries' ? '/api/user/injuries' : '/api/user/training-records'
    const url = isEdit ? `${endpoint}/${item.id}` : endpoint
    const method = isEdit ? 'PUT' : 'POST'
    const body = tab === 'injuries'
      ? { ...form, userId }
      : { ...form, userId, sets: form.sets || null, reps: form.reps || null, weight: form.weight || null, duration: form.duration || null, caloriesBurned: form.caloriesBurned || null }
    try {
      const res = await fetch(url, { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
      const data = await res.json()
      if (data.success) onSaved()
    } catch (err) { console.error(err) }
    finally { setSaving(false) }
  }

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
      <div className="bg-white rounded-2xl p-6 w-full max-w-lg shadow-xl">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-gray-800">{isEdit ? '编辑' : '新增'}{tab === 'injuries' ? '伤病' : '训练'}记录</h3>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-xl leading-none">&times;</button>
        </div>
        <form onSubmit={handleSubmit} className="space-y-3">
          {tab === 'injuries' ? (
            <InjuryFormFields form={form} updateField={updateField} inputCls={inputCls} />
          ) : (
            <TrainingFormFields form={form} updateField={updateField} inputCls={inputCls} />
          )}
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onClose} className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800">取消</button>
            <button type="submit" disabled={saving}
              className="px-4 py-2 bg-indigo-500 text-white rounded-lg text-sm hover:bg-indigo-600 disabled:bg-gray-300 transition-colors">
              {saving ? '保存中...' : '保存'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

function InjuryFormFields({ form, updateField, inputCls }) {
  return (
    <div className="grid grid-cols-2 gap-3">
      <div>
        <label className="block text-xs text-gray-600 mb-1">伤病类型 *</label>
        <input value={form.injuryType} onChange={(e) => updateField('injuryType', e.target.value)}
          placeholder="如：肌肉拉伤" className={inputCls} />
      </div>
      <div>
        <label className="block text-xs text-gray-600 mb-1">伤病部位 *</label>
        <input value={form.injuryLocation} onChange={(e) => updateField('injuryLocation', e.target.value)}
          placeholder="如：右肩" className={inputCls} />
      </div>
      <div>
        <label className="block text-xs text-gray-600 mb-1">严重程度</label>
        <select value={form.severity} onChange={(e) => updateField('severity', e.target.value)} className={inputCls}>
          {SEVERITY_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
        </select>
      </div>
      <div>
        <label className="block text-xs text-gray-600 mb-1">恢复状态</label>
        <select value={form.recoveryStatus} onChange={(e) => updateField('recoveryStatus', e.target.value)} className={inputCls}>
          {RECOVERY_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
        </select>
      </div>
      <div>
        <label className="block text-xs text-gray-600 mb-1">受伤日期</label>
        <input type="date" value={form.injuryDate} onChange={(e) => updateField('injuryDate', e.target.value)} className={inputCls} />
      </div>
      <div className="col-span-2">
        <label className="block text-xs text-gray-600 mb-1">详细描述</label>
        <input value={form.description} onChange={(e) => updateField('description', e.target.value)}
          placeholder="描述伤病情况" className={inputCls} />
      </div>
    </div>
  )
}

function TrainingFormFields({ form, updateField, inputCls }) {
  return (
    <div className="grid grid-cols-2 gap-3">
      <div>
        <label className="block text-xs text-gray-600 mb-1">训练日期 *</label>
        <input type="date" value={form.trainingDate} onChange={(e) => updateField('trainingDate', e.target.value)} className={inputCls} />
      </div>
      <div>
        <label className="block text-xs text-gray-600 mb-1">训练类型 *</label>
        <input value={form.trainingType} onChange={(e) => updateField('trainingType', e.target.value)}
          placeholder="如：力量训练" className={inputCls} />
      </div>
      <div>
        <label className="block text-xs text-gray-600 mb-1">运动名称 *</label>
        <input value={form.exerciseName} onChange={(e) => updateField('exerciseName', e.target.value)}
          placeholder="如：卧推" className={inputCls} />
      </div>
      <div>
        <label className="block text-xs text-gray-600 mb-1">完成状态</label>
        <select value={form.completionStatus} onChange={(e) => updateField('completionStatus', e.target.value)} className={inputCls}>
          {COMPLETION_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
        </select>
      </div>
      <div>
        <label className="block text-xs text-gray-600 mb-1">组数</label>
        <input type="number" value={form.sets} onChange={(e) => updateField('sets', e.target.value)} placeholder="4" className={inputCls} />
      </div>
      <div>
        <label className="block text-xs text-gray-600 mb-1">次数</label>
        <input type="number" value={form.reps} onChange={(e) => updateField('reps', e.target.value)} placeholder="10" className={inputCls} />
      </div>
      <div>
        <label className="block text-xs text-gray-600 mb-1">重量(kg)</label>
        <input type="number" step="0.01" value={form.weight} onChange={(e) => updateField('weight', e.target.value)} placeholder="60" className={inputCls} />
      </div>
      <div>
        <label className="block text-xs text-gray-600 mb-1">时长(分钟)</label>
        <input type="number" value={form.duration} onChange={(e) => updateField('duration', e.target.value)} placeholder="30" className={inputCls} />
      </div>
      <div>
        <label className="block text-xs text-gray-600 mb-1">消耗卡路里</label>
        <input type="number" value={form.caloriesBurned} onChange={(e) => updateField('caloriesBurned', e.target.value)} placeholder="200" className={inputCls} />
      </div>
      <div className="col-span-2">
        <label className="block text-xs text-gray-600 mb-1">备注</label>
        <input value={form.notes} onChange={(e) => updateField('notes', e.target.value)} placeholder="训练感受" className={inputCls} />
      </div>
    </div>
  )
}
