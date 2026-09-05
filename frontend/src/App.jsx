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
  const [form, setForm] = useState(emptyForm)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [analyzingId, setAnalyzingId] = useState(null)
  const [analysisResults, setAnalysisResults] = useState({})

  const loadData = async () => {
    setLoading(true)
    setError('')

    try {
      const [healthRes, paymentsRes, revenueRes] = await Promise.all([
        fetch('/api/health'),
        fetch('/api/payments'),
        fetch('/api/revenue-at-risk'),
      ])

      if (!healthRes.ok || !paymentsRes.ok || !revenueRes.ok) {
        throw new Error('Backend is not reachable')
      }

      setHealth(await healthRes.json())
      setPayments(await paymentsRes.json())
      setRevenueAtRisk(await revenueRes.json())
    } catch (err) {
      setError(err.message || 'Failed to load data from backend')
      setHealth(null)
      setPayments([])
      setRevenueAtRisk(null)
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

    try {
      const response = await fetch(`/api/ai/analyze/${paymentId}`, {
        method: 'POST',
      })

      if (!response.ok) {
        const data = await response.json().catch(() => ({}))
        throw new Error(data.message || 'AI analysis failed')
      }

      const result = await response.json()
      setAnalysisResults((current) => ({
        ...current,
        [paymentId]: result,
      }))
      setMessage(`AI analysis complete for ${paymentId}`)
    } catch (err) {
      setError(err.message || `Failed to analyze ${paymentId}`)
    } finally {
      setAnalyzingId(null)
    }
  }

  return (
    <div className="app">
      <header className="hero">
        <p className="eyebrow">Razorpay Recovery</p>
        <h1>PayRecover AI</h1>
        <p className="subtitle">Monitor failed payments and get AI recovery recommendations.</p>
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
                  <th>Reason</th>
                  <th>Email</th>
                  <th>AI Action</th>
                </tr>
              </thead>
              <tbody>
                {payments.map((payment) => (
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
                    <td>{payment.failureReason || '-'}</td>
                    <td>{payment.customerEmail || '-'}</td>
                    <td>
                      {payment.status?.toUpperCase() === 'FAILED' ? (
                        <button
                          type="button"
                          className="analyze-btn"
                          onClick={() => handleAnalyze(payment.paymentId)}
                          disabled={analyzingId === payment.paymentId}
                        >
                          {analyzingId === payment.paymentId ? 'Analyzing...' : 'Analyze'}
                        </button>
                      ) : (
                        '-'
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {Object.keys(analysisResults).length > 0 && (
        <section className="panel">
          <h2>AI Recommendations</h2>
          <div className="analysis-list">
            {Object.entries(analysisResults).map(([paymentId, result]) => (
              <div key={paymentId} className="analysis-card">
                <div className="analysis-header">
                  <strong>{paymentId}</strong>
                  <span className={`badge action-${result.action?.toLowerCase()}`}>
                    {result.action}
                  </span>
                </div>
                <p>{result.reason}</p>
                <p className="confidence">
                  Confidence: {(result.confidence * 100).toFixed(0)}%
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
