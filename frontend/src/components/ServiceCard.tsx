import { Link } from 'react-router-dom'
import { formatDuration, formatPrice } from '../utils/format'
import type { ServiceItem } from '../types'

export default function ServiceCard({ service }: { service: ServiceItem }) {
  return (
    <article className="service-card">
      {service.imageUrl ? (
        <img src={service.imageUrl} alt={service.name} loading="lazy" className="service-img" />
      ) : (
        <div className="service-img placeholder" aria-hidden="true">
          {service.name.charAt(0)}
        </div>
      )}
      <div className="service-body">
        <h3>{service.name}</h3>
        {service.description && <p className="muted">{service.description}</p>}
        <div className="service-meta">
          <strong>{formatPrice(service.price)}</strong>
          <span className="muted">⏱ {formatDuration(service.durationMin)}</span>
        </div>
        <Link to={`/book?service=${service.id}`} className="btn btn-outline btn-small">
          Book this
        </Link>
      </div>
    </article>
  )
}
