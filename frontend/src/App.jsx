import { useEffect, useState } from 'react'
import './App.css'

const emptyForm = {
  paymentId: '',
  amount: '',
  currency: 'INR',
  status: 'FAILED',
  failureReason: '',
  customerEmail: '',
}

function App() {
  const [health, setHealth] = useState(null)
  const [payments, setPayments] = useState([])
  const [revenueAtRisk, setRevenueAtRisk] = useState(null)
  const [recoveryHistory, setRecoveryHistory] = useState([])
  const [form, setForm] = useState(emptyForm)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [analyzingId, setAnalyzingId] = useState(null)
  const [executingId, setExecutingId] = useState(null)
  const [recommendations, setRecommendations] = useState({})

  const loadData = async () => {
    setLoading(true)
    setError('')

    try {
      const [healthRes, paymentsRes, revenueRes, historyRes] = await Promise.all([
        fetch('/api/health'),
        fetch('/api/payments'),
        fetch('/api/revenue-at-risk'),
        fetch('/api/recovery/history'),
      ])

      if (!healthRes.ok || !paymentsRes.ok || !revenueRes.ok) {
        throw new Error('Backend is not reachable')
      }

      setHealth(await healthRes.json())
      setPayments(await paymentsRes.json())
      setRevenueAtRisk(await revenueRes.json())

      if (historyRes.ok) {
        const history = await historyRes.json()
        setRecoveryHistory(history)

        const latestByPayment = {}
        history.forEach((item) => {
          if (
            !latestByPayment[item.paymentId] ||
            item.executionStatus === 'RECOMMENDED'
          ) {
            latestByPayment[item.paymentId] = item
          }
        })
        setRecommendations(latestByPayment)
      }
    } catch (err) {
      setError(err.message || 'Failed to load data from backend')
      setHealth(null)
      setPayments([])
      setRevenueAtRisk(null)
      setRecoveryHistory([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [])

  const handleChange = (event) => {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setMessage('')
    setError('')

    try {
      const response = await fetch('/api/payments', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          paymentId: form.paymentId,
          amount: Number(form.amount),
          currency: form.currency,
          status: form.status,
          failureReason: form.failureReason || null,
          customerEmail: form.customerEmail || null,
          attemptCount: 1,
        }),
      })

      if (!response.ok) {
        throw new Error('Could not create payment')
      }

      setForm(emptyForm)
      setMessage('Payment saved successfully')
      await loadData()
    } catch (err) {
      setError(err.message || 'Failed to create payment')
    }
  }

  const handleAnalyze = async (paymentId) => {
    setAnalyzingId(paymentId)
    setError('')
    setMessage('')

    try {
      const response = await fetch(`/api/ai/analyze/${paymentId}`, {
        method: 'POST',
      })

      if (!response.ok) {
        const data = await response.json().catch(() => ({}))
        throw new Error(data.message || 'AI analysis failed')
      }

      const result = await response.json()
      setRecommendations((current) => ({
        ...current,
        [paymentId]: result,
      }))
      setMessage(`AI recommends ${result.action} for ${paymentId}. Click Execute to apply.`)
      await loadData()
    } catch (err) {
      setError(err.message || `Failed to analyze ${paymentId}`)
    } finally {
      setAnalyzingId(null)
    }
  }

  const handleExecute = async (paymentId) => {
    setExecutingId(paymentId)
    setError('')
    setMessage('')

    try {
      const response = await fetch(`/api/recovery/execute/${paymentId}`, {
        method: 'POST',
      })

      if (!response.ok) {
        const data = await response.json().catch(() => ({}))
        throw new Error(data.message || 'Failed to execute recovery action')
      }

      const result = await response.json()
      setMessage(result.executionMessage || `Executed ${result.action} for ${paymentId}`)
      await loadData()
    } catch (err) {
      setError(err.message || `Failed to execute action for ${paymentId}`)
    } finally {
      setExecutingId(null)
    }
  }

  const getRecommendation = (paymentId) => recommendations[paymentId]

  return (
    <div className="app">
      <header className="hero">
        <p className="eyebrow">Razorpay Recovery</p>
        <h1>PayRecover AI</h1>
        <p className="subtitle">Analyze failed payments and execute AI recovery actions.</p>
      </header>

      <section className="panel">
        <div className="panel-header">
          <h2>Dashboard</h2>
          <button type="button" className="refresh" onClick={loadData}>
            Refresh
          </button>
        </div>

        {loading && <p>Loading...</p>}
        {!loading && error && <p className="error">{error}</p>}
        {!loading && message && <p className="message">{message}</p>}

        {!loading && health && (
          <div className="stats-grid">
            <div className="stat-card up">
              <span>API Health</span>
              <strong>{health.status}</strong>
            </div>
            {revenueAtRisk && (
              <>
                <div className="stat-card risk">
                  <span>Revenue at Risk</span>
                  <strong>₹{revenueAtRisk.totalAtRiskAmount}</strong>
                </div>
                <div className="stat-card">
                  <span>Failed Payments</span>
                  <strong>{revenueAtRisk.failedPaymentCount}</strong>
                </div>
                <div className="stat-card">
                  <span>Eligible for Recovery</span>
                  <strong>{revenueAtRisk.eligiblePaymentCount}</strong>
                </div>
              </>
            )}
          </div>
        )}
      </section>

      <section className="panel">
        <h2>Add Failed Payment</h2>
        <form className="payment-form" onSubmit={handleSubmit}>
          <input
            name="paymentId"
            placeholder="Payment ID"
            value={form.paymentId}
            onChange={handleChange}
            required
          />
          <input
            name="amount"
            type="number"
            step="0.01"
            placeholder="Amount"
            value={form.amount}
            onChange={handleChange}
            required
          />
          <input
            name="currency"
            placeholder="Currency"
            value={form.currency}
            onChange={handleChange}
            required
          />
          <select name="status" value={form.status} onChange={handleChange}>
            <option value="FAILED">FAILED</option>
            <option value="PENDING">PENDING</option>
            <option value="RECOVERED">RECOVERED</option>
          </select>
          <input
            name="failureReason"
            placeholder="Failure reason"
            value={form.failureReason}
            onChange={handleChange}
          />
          <input
            name="customerEmail"
            type="email"
            placeholder="Customer email"
            value={form.customerEmail}
            onChange={handleChange}
          />
          <button type="submit">Save Payment</button>
        </form>
      </section>

      <section className="panel">
        <h2>Payments ({payments.length})</h2>
        {payments.length === 0 ? (
          <p className="empty">No payments yet. Add one above.</p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Payment ID</th>
                  <th>Amount</th>
                  <th>Status</th>
                  <th>Attempts</th>
                  <th>Reason</th>
                  <th>AI Recommendation</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {payments.map((payment) => {
                  const recommendation = getRecommendation(payment.paymentId)
                  const canAnalyze = payment.status?.toUpperCase() === 'FAILED'
                  const canExecute =
                    recommendation?.executionStatus === 'RECOMMENDED' && canAnalyze

                  return (
                    <tr key={payment.id}>
                      <td>{payment.paymentId}</td>
                      <td>
                        {payment.amount} {payment.currency}
                      </td>
                      <td>
                        <span className={`badge ${payment.status?.toLowerCase()}`}>
                          {payment.status}
                        </span>
                      </td>
                      <td>{payment.attemptCount ?? 0}</td>
                      <td>{payment.failureReason || '-'}</td>
                      <td>
                        {recommendation ? (
                          <div className="recommendation-cell">
                            <span
                              className={`badge action-${recommendation.action?.toLowerCase()}`}
                            >
                              {recommendation.action}
                            </span>
                            <span className="recommendation-status">
                              {recommendation.executionStatus}
                            </span>
                          </div>
                        ) : (
                          '-'
                        )}
                      </td>
                      <td>
                        <div className="action-buttons">
                          {canAnalyze && (
                            <button
                              type="button"
                              className="analyze-btn"
                              onClick={() => handleAnalyze(payment.paymentId)}
                              disabled={analyzingId === payment.paymentId}
                            >
                              {analyzingId === payment.paymentId
                                ? 'Analyzing...'
                                : 'Analyze'}
                            </button>
                          )}
                          {canExecute && (
                            <button
                              type="button"
                              className="execute-btn"
                              onClick={() => handleExecute(payment.paymentId)}
                              disabled={executingId === payment.paymentId}
                            >
                              {executingId === payment.paymentId
                                ? 'Executing...'
                                : 'Execute'}
                            </button>
                          )}
                          {!canAnalyze && !canExecute && '-'}
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {recoveryHistory.length > 0 && (
        <section className="panel">
          <h2>Recovery History</h2>
          <div className="analysis-list">
            {recoveryHistory.map((item) => (
              <div key={item.id} className="analysis-card">
                <div className="analysis-header">
                  <strong>{item.paymentId}</strong>
                  <span className={`badge action-${item.action?.toLowerCase()}`}>
                    {item.action}
                  </span>
                  <span className={`badge status-${item.executionStatus?.toLowerCase()}`}>
                    {item.executionStatus}
                  </span>
                </div>
                <p>{item.reason}</p>
                {item.executionMessage && (
                  <p className="execution-message">{item.executionMessage}</p>
                )}
                <p className="confidence">
                  Confidence: {(item.confidence * 100).toFixed(0)}%
                  {item.executedAt && ` · Executed: ${new Date(item.executedAt).toLocaleString()}`}
                </p>
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  )
}

export default App
