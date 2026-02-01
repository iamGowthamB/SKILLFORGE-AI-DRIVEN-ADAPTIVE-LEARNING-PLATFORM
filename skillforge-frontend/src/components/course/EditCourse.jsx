import React, { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useSelector } from 'react-redux'
import { courseService } from '../../services/courseService'
import { ArrowLeft, Save } from 'lucide-react'
import Card from '../common/Card'
import Input from '../common/Input'
import Button from '../common/Button'
import Loader from '../common/Loader'
import toast from 'react-hot-toast'

const EditCourse = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const { user } = useSelector((state) => state.auth)

  const [loading, setLoading] = useState(true)
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    difficultyLevel: 'BEGINNER',
    duration: '',
    thumbnailUrl: '',
    category: '',
    tags: ''
  })

  useEffect(() => {
    fetchCourseData()
  }, [id])

  const fetchCourseData = async () => {
    try {
      setLoading(true)
      const response = await courseService.getCourseById(id, null)

      // Handle different response structures
      const courseData = response?.data?.data || response?.data || response

      console.log('EditCourse - Full response:', response)
      console.log('EditCourse - Extracted courseData:', courseData)

      if (courseData) {
        setFormData({
          title: courseData.title || '',
          description: courseData.description || '',
          difficultyLevel: courseData.difficultyLevel || 'BEGINNER',
          duration: courseData.duration || '',
          thumbnailUrl: courseData.thumbnailUrl || '',
          category: courseData.category || '',
          tags: courseData.tags || ''
        })
      }
    } catch (error) {
      console.error('Error fetching course:', error)
      toast.error('Failed to load course data')
    } finally {
      setLoading(false)
    }
  }

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value })
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      setLoading(true)
      await courseService.updateCourse(id, formData)
      toast.success('Course updated successfully!')
      navigate(`/courses/${id}`)
    } catch (error) {
      console.error('Error updating course:', error)
      toast.error('Failed to update course')
    } finally {
      setLoading(false)
    }
  }

  const handleCancel = () => {
    navigate(`/courses/${id}`)
  }

  if (loading) return <Loader />

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="mb-8">
        <button
          onClick={handleCancel}
          className="flex items-center space-x-2 text-gray-600 hover:text-gray-900 mb-4 transition-colors"
        >
          <ArrowLeft size={20} />
          <span>Back to Course</span>
        </button>
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Edit Course</h1>
        <p className="text-gray-600">Update your course details</p>
      </div>

      <Card className="p-6 md:p-8">
        <form onSubmit={handleSubmit} className="space-y-6">
          <Input
            label="Course Title"
            type="text"
            name="title"
            value={formData.title}
            onChange={handleChange}
            placeholder="e.g., Introduction to Web Development"
            required
          />

          <div className="space-y-1">
            <label className="block text-sm font-medium text-gray-700">
              Description <span className="text-red-500">*</span>
            </label>
            <textarea
              name="description"
              value={formData.description}
              onChange={handleChange}
              placeholder="Describe what students will learn in this course..."
              rows="4"
              className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
              required
            />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-1">
              <label className="block text-sm font-medium text-gray-700">
                Difficulty Level <span className="text-red-500">*</span>
              </label>
              <select
                name="difficultyLevel"
                value={formData.difficultyLevel}
                onChange={handleChange}
                className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                required
              >
                <option value="BEGINNER">Beginner</option>
                <option value="INTERMEDIATE">Intermediate</option>
                <option value="ADVANCED">Advanced</option>
              </select>
            </div>

            <Input
              label="Duration (minutes)"
              type="number"
              name="duration"
              value={formData.duration}
              onChange={handleChange}
              placeholder="e.g., 120"
            />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-1">
              <label className="block text-sm font-medium text-gray-700">
                Category <span className="text-red-500">*</span>
              </label>
              <select
                name="category"
                value={formData.category}
                onChange={handleChange}
                className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                required
              >
                <option value="">Select Category</option>
                <option value="DEVELOPMENT">Development</option>
                <option value="BUSINESS">Business</option>
                <option value="FINANCE">Finance</option>
                <option value="IT_SOFTWARE">IT & Software</option>
                <option value="MARKETING">Marketing</option>
                <option value="DESIGN">Design</option>
                <option value="PERSONAL_DEVELOPMENT">Personal Development</option>
              </select>
            </div>
            <Input
              label="Tags (comma separated)"
              type="text"
              name="tags"
              value={formData.tags}
              onChange={handleChange}
              placeholder="e.g., java, spring boot, backend"
            />
          </div>

          <Input
            label="Thumbnail URL (Optional)"
            type="url"
            name="thumbnailUrl"
            value={formData.thumbnailUrl}
            onChange={handleChange}
            placeholder="https://example.com/image.jpg"
          />

          <div className="flex items-center space-x-4 pt-4">
            <Button
              type="submit"
              variant="primary"
              size="lg"
              disabled={loading}
              icon={Save}
            >
              {loading ? 'Updating...' : 'Update Course'}
            </Button>
            <Button
              type="button"
              variant="secondary"
              size="lg"
              onClick={handleCancel}
            >
              Cancel
            </Button>
          </div>
        </form>
      </Card>
    </div>
  )
}

export default EditCourse